<template>
    <article
        v-if="message"
        class="mail-thread d-flex flex-column overflow-x-hidden"
        :aria-label="$t('mail.application.region.messagethread')"
    >
        <bm-alert-area :alerts="alerts" @remove="REMOVE">
            <template v-slot="context">
                <component :is="context.alert.renderer" :alert="context.alert" />
            </template>
        </bm-alert-area>
        <mail-composer v-if="isADraft" :message-key="currentMessageKey" />
        <mail-viewer v-else :message-key="currentMessageKey" />
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

import { RESET_ACTIVE_MESSAGE, SET_ACTIVE_FOLDER, SET_BLOCK_REMOTE_IMAGES, SET_MESSAGE_COMPOSING } from "~mutations";
import { MESSAGE_IS_LOADED, MY_DRAFTS, MY_MAILBOX } from "~getters";
import { FETCH_MESSAGE_IF_NOT_LOADED } from "~actions";
import BlockedRemoteContent from "./Alerts/BlockedRemoteContent";
import VideoConferencing from "./Alerts/VideoConferencing";
import MailComposer from "../MailComposer";
import MailViewer from "../MailViewer";
import MailViewerLoading from "../MailViewer/MailViewerLoading";
import MessagePathParam from "../../router/MessagePathParam";
import { WaitForMixin, ComposerInitMixin } from "~mixins";
import { LoadingStatus } from "../../model/loading-status";
import { isNewMessage } from "../../model/draft";

export default {
    name: "MailThread",
    components: {
        BlockedRemoteContent,
        BmAlertArea,
        MailComposer,
        MailViewer,
        MailViewerLoading,
        VideoConferencing
    },
    mixins: [ComposerInitMixin, WaitForMixin],
    provide: {
        area: "mail-thread"
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["activeFolder", "folders", "messages"]),
        ...mapGetters("mail", { MY_MAILBOX, MESSAGE_IS_LOADED, MY_DRAFTS }),

        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "mail-thread") }),
        message() {
            return this.MESSAGE_IS_LOADED(this.currentMessageKey) && this.messages[this.currentMessageKey];
        },
        folder() {
            return this.message && this.folders[this.message.folderRef.key];
        },
        isADraft() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
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
        async currentMessageKey(value) {
            this.SET_BLOCK_REMOTE_IMAGES(false);
            try {
                const message = this.messages[value];
                const folderKey = message.folderRef.key;
                if (!message.composing) {
                    this.SET_ACTIVE_FOLDER(this.folders[folderKey]);
                    if (this.MY_DRAFTS && folderKey === this.MY_DRAFTS.key) {
                        this.SET_MESSAGE_COMPOSING({ messageKey: value, composing: true });
                    }
                }
            } catch (e) {
                console.log(e);
                this.$router.push({ name: "mail:home" });
            }
        },

        "$route.params.messagepath": {
            async handler(value) {
                this.RESET_ACTIVE_MESSAGE();
                if (value) {
                    try {
                        let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                        await this.$waitFor(MY_MAILBOX, assert);
                        const { folderKey, messageId: internalId } = MessagePathParam.parse(value, this.activeFolder);
                        if (isNewMessage({ remoteRef: { internalId } })) {
                            if (this.$route.query?.action && this.$route.query?.message) {
                                const { action, message: related } = this.$route.query;
                                await this.initRelatedMessage(action, MessagePathParam.parse(related));
                            } else {
                                this.initNewMessage();
                            }
                        }
                        const { key } = await this.FETCH_MESSAGE_IF_NOT_LOADED({
                            internalId,
                            folder: this.folders[folderKey]
                        });
                        this.$store.commit("mail-webapp/currentMessage/update", { key: key });
                    } catch (e) {
                        console.log(e);
                        this.$router.push({ name: "mail:home" });
                    }
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            SET_ACTIVE_FOLDER,
            SET_BLOCK_REMOTE_IMAGES,
            SET_MESSAGE_COMPOSING
        }),
        ...mapActions("mail", { FETCH_MESSAGE_IF_NOT_LOADED }),
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
