<template>
    <article
        v-if="CONVERSATION_IS_LOADED(conversation)"
        class="mail-thread d-flex flex-column overflow-x-hidden bg-surface"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <mail-conversation-viewer :conversation="conversation" />
        <div />
    </article>
    <article v-else class="mail-thread">
        <mail-viewer-loading />
    </article>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { INFO, REMOVE } from "@bluemind/alert.store";
import { BmAlertArea } from "@bluemind/styleguide";

import {
    RESET_ACTIVE_MESSAGE,
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_BLOCK_REMOTE_IMAGES,
    SET_CURRENT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import {
    CONVERSATION_LIST_IS_SEARCH_MODE,
    CONVERSATION_METADATA,
    CONVERSATION_IS_LOADED,
    MY_DRAFTS,
    MY_MAILBOX,
    SELECTION_IS_EMPTY
} from "~/getters";
import { FETCH_CONVERSATION_IF_NOT_LOADED } from "~/actions";
import BlockedRemoteContent from "./Alerts/BlockedRemoteContent";
import VideoConferencing from "./Alerts/VideoConferencing";
import MailComposer from "../MailComposer";
import MailConversationViewer from "../MailViewer/MailConversationViewer";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import ConversationPathParam from "~/router/ConversationPathParam";
import { WaitForMixin, ComposerInitMixin } from "~/mixins";
import { isDraftFolder } from "~/model/folder";
import { LoadingStatus } from "~/model/loading-status";
import { MessageCreationModes } from "~/model/message";

export default {
    name: "MailThread",
    components: {
        BlockedRemoteContent,
        BmAlertArea,
        MailComposer,
        MailConversationViewer,
        MailViewer,
        MailViewerLoading,
        VideoConferencing
    },
    mixins: [ComposerInitMixin, WaitForMixin],
    provide: {
        area: "mail-thread"
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("mail", {
            messages: ({ conversations }) => conversations.messages,
            conversations: ({ conversations }) => conversations.conversationByKey,
            currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        ...mapGetters("mail", {
            CONVERSATION_LIST_IS_SEARCH_MODE,
            CONVERSATION_METADATA,
            MY_MAILBOX,
            CONVERSATION_IS_LOADED,
            MY_DRAFTS,
            SELECTION_IS_EMPTY
        }),
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-thread") }),
        conversation() {
            return this.CONVERSATION_METADATA(this.currentConversation?.key);
        },
        folder() {
            return this.conversation && this.folders[this.conversation.folderRef.key];
        },
        isADraft() {
            return isDraftFolder(this.folder.path);
        },
        readOnlyAlert() {
            return {
                alert: { name: "mail.READ_ONLY_FOLDER", uid: "READ_ONLY_FOLDER" },
                options: { area: "mail-thread", renderer: "DefaultAlert" }
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
        async "currentConversation.key"() {
            this.SET_BLOCK_REMOTE_IMAGES(false);
        },
        "$route.params.conversationpath": {
            async handler(conversationPath) {
                this.RESET_PARTS_DATA();
                if (conversationPath) {
                    try {
                        this.RESET_ACTIVE_MESSAGE();
                        this.UNSET_CURRENT_CONVERSATION();
                        let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                        await this.$waitFor(MY_MAILBOX, assert);

                        const {
                            folderKey,
                            internalId,
                            action,
                            relatedFolderKey,
                            relatedId
                        } = ConversationPathParam.parse(conversationPath, this.activeFolder);

                        if (!this.SELECTION_IS_EMPTY) {
                            this.UNSELECT_ALL_CONVERSATIONS();
                        }

                        switch (action) {
                            case MessageCreationModes.REPLY:
                            case MessageCreationModes.REPLY_ALL:
                            case MessageCreationModes.FORWARD:
                                await this.initRelatedMessage(action, {
                                    internalId: relatedId,
                                    folderKey: relatedFolderKey
                                });
                                break;
                            case MessageCreationModes.NEW:
                                this.initNewMessage();
                                break;
                            default:
                                break;
                        }

                        const conversation = await this.FETCH_CONVERSATION_IF_NOT_LOADED({
                            conversationId: internalId,
                            folder: this.folders[folderKey]
                        });

                        if (conversation) {
                            this.SET_CURRENT_CONVERSATION(conversation);
                        }
                    } catch (e) {
                        this.$router.push({ name: "mail:home" });
                        throw e;
                    }
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_PARTS_DATA,
            RESET_ACTIVE_MESSAGE,
            SET_ACTIVE_MESSAGE,
            SET_BLOCK_REMOTE_IMAGES,
            SET_CURRENT_CONVERSATION,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_CONVERSATION_IF_NOT_LOADED }),
        ...mapActions("alert", { REMOVE, INFO })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-thread {
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
