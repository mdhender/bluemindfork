<template>
    <article
        v-if="ACTIVE_MESSAGE"
        class="mail-message d-flex flex-column overflow-x-hidden"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <template v-if="ACTIVE_MESSAGE.composing">
            <mail-composer v-if="DEFAULT_IDENTITY" :message="ACTIVE_MESSAGE" />
            <mail-composer-loading v-else />
        </template>

        <mail-viewer v-else :message="ACTIVE_MESSAGE" />
        <div />
    </article>
    <article v-else class="mail-message">
        <mail-viewer-loading />
    </article>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/styleguide";

import {
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_ACTIVE_FOLDER,
    SET_BLOCK_REMOTE_IMAGES,
    SET_CURRENT_CONVERSATION,
    SET_MESSAGE_COMPOSING,
    UNSELECT_ALL_CONVERSATIONS,
    RESET_ACTIVE_MESSAGE,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import {
    ACTIVE_MESSAGE,
    CONVERSATION_LIST_IS_SEARCH_MODE,
    MY_DRAFTS,
    MY_MAILBOX,
    SELECTION_IS_EMPTY,
    CONVERSATION_LIST_UNREAD_FILTER_ENABLED
} from "~/getters";
import { FETCH_MESSAGE_IF_NOT_LOADED, MARK_CONVERSATIONS_AS_READ } from "~/actions";
import BlockedRemoteContent from "./Alerts/BlockedRemoteContent";
import VideoConferencing from "./Alerts/VideoConferencing";
import MailComposer from "../MailComposer";
import MailComposerLoading from "../MailComposer/MailComposerLoading";
import MailConversationViewer from "../MailViewer/MailConversationViewer";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import MessagePathParam from "~/router/MessagePathParam";
import { WaitForMixin, ComposerInitMixin } from "~/mixins";
import { isDraftFolder } from "~/model/folder";
import { LoadingStatus } from "~/model/loading-status";
import { isNewMessage } from "~/model/draft";

export default {
    name: "MailMessage",
    components: {
        BlockedRemoteContent,
        BmAlertArea,
        MailComposer,
        MailComposerLoading,
        MailConversationViewer,
        MailViewer,
        MailViewerLoading,
        VideoConferencing
    },
    mixins: [ComposerInitMixin, WaitForMixin],
    provide: {
        area: "mail-message"
    },
    computed: {
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("mail", { conversationByKey: ({ conversations }) => conversations.conversationByKey }),
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        ...mapGetters("mail", {
            ACTIVE_MESSAGE,
            CONVERSATION_LIST_IS_SEARCH_MODE,
            MY_MAILBOX,
            MY_DRAFTS,
            SELECTION_IS_EMPTY,
            CONVERSATION_LIST_UNREAD_FILTER_ENABLED
        }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-message") }),
        folder() {
            return this.ACTIVE_MESSAGE && this.folders[this.ACTIVE_MESSAGE.folderRef.key];
        },
        isADraft() {
            return isDraftFolder(this.folder.path);
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "mail-message", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        "folder.key"() {
            if (this.folder && !this.folder.writable) {
                this.INFO(this.readOnlyAlert);
            } else {
                this.REMOVE(this.readOnlyAlert.alert);
            }
        },
        async "ACTIVE_MESSAGE.key"() {
            this.SET_BLOCK_REMOTE_IMAGES(false);
            try {
                if (this.ACTIVE_MESSAGE && !this.ACTIVE_MESSAGE.composing) {
                    const folderKey = this.ACTIVE_MESSAGE.folderRef.key;
                    if (this.MY_DRAFTS && folderKey === this.MY_DRAFTS.key) {
                        this.SET_MESSAGE_COMPOSING({ messageKey: this.ACTIVE_MESSAGE.key, composing: true });
                    }
                    if (this.CONVERSATION_LIST_IS_SEARCH_MODE) {
                        this.SET_ACTIVE_FOLDER(this.folders[folderKey]);
                    }
                }
            } catch (e) {
                this.$router.push({ name: "mail:home" });
                throw e;
            }
        },

        "$route.params.messagepath": {
            async handler(messagepath, oldMessagepath) {
                if (oldMessagepath) {
                    const { internalId: oldInternalId } = MessagePathParam.parse(oldMessagepath, this.activeFolder);
                    if (isNewMessage({ remoteRef: { internalId: oldInternalId } })) {
                        // preserve composer state for 1st save (route is changed only to have a valid route)
                        return;
                    }
                }
                this.RESET_PARTS_DATA();
                this.RESET_ACTIVE_MESSAGE();
                this.UNSET_CURRENT_CONVERSATION();
                try {
                    let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                    await this.$waitFor(MY_MAILBOX, assert);
                    const { folderKey, internalId } = MessagePathParam.parse(messagepath, this.activeFolder);
                    let message;
                    if (isNewMessage({ remoteRef: { internalId } })) {
                        if (this.$route.query?.action && this.$route.query?.message) {
                            const { action, message: related } = this.$route.query;
                            message = await this.initRelatedMessage(action, MessagePathParam.parse(related));
                        } else {
                            message = this.initNewMessage();
                        }
                    } else {
                        message = await this.FETCH_MESSAGE_IF_NOT_LOADED({
                            internalId,
                            folder: this.folders[folderKey]
                        });
                    }
                    // FIXME !! once message.conversationRef.key is always valid, remove this if / else
                    // if conversations mode is not activate, then message.conversationRef.key is false
                    const conversationsActivated =
                        this.settings.mail_thread === "true" && this.folders[this.activeFolder].allowConversations;
                    const conversationKey =
                        conversationsActivated && !message.composing ? message.conversationRef.key : message.key;
                    if (!message.composing) {
                        await this.$waitFor(
                            () => this.conversationByKey[conversationKey],
                            conversation => Boolean(conversation)
                        );
                    }
                    const conversation = this.conversationByKey[conversationKey];
                    // FIXME ? conversation is null if message.composing
                    this.SET_CURRENT_CONVERSATION(conversation);
                    this.SET_ACTIVE_MESSAGE(message);

                    if (!this.SELECTION_IS_EMPTY) {
                        this.UNSELECT_ALL_CONVERSATIONS();
                    }
                    if (
                        !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                        this.folders[this.activeFolder].writable &&
                        !message.composing
                    ) {
                        this.MARK_CONVERSATIONS_AS_READ({ conversations: [conversation], noAlert: true });
                    }
                } catch (e) {
                    this.$router.push({ name: "mail:home" });
                    throw e;
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            RESET_PARTS_DATA,
            SET_ACTIVE_FOLDER,
            SET_ACTIVE_MESSAGE,
            SET_BLOCK_REMOTE_IMAGES,
            SET_CURRENT_CONVERSATION,
            SET_MESSAGE_COMPOSING,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_MESSAGE_IF_NOT_LOADED, MARK_CONVERSATIONS_AS_READ }),
        ...mapActions("alert", { REMOVE, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-message {
    min-height: 100%;

    .mail-composer ~ .mail-viewer {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            display: none !important;
        }
    }

    .mail-composer {
        @media (min-width: map-get($grid-breakpoints, "lg")) {
            height: auto !important;
        }
    }
}

.overflow-x-hidden {
    overflow-x: hidden;
}
</style>
