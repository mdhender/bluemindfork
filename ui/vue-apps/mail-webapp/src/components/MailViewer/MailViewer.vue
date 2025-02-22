<template>
    <section class="mail-viewer d-flex flex-column flex-grow-1 bg-surface">
        <bm-extension id="webapp.mail" path="viewer.header" :message="message" />
        <mail-viewer-toolbar class="desktop-only justify-content-end" :message="message" :conversation="conversation" />
        <mail-viewer-content :message="message" @remote-content="setBlockRemote" />
        <mail-viewer-toolbar
            class="mobile-only justify-content-around"
            :message="message"
            :conversation="conversation"
        />
    </section>
</template>

<script>
import { mapState, mapActions, mapGetters, mapMutations } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { REMOVE, WARNING } from "@bluemind/alert.store";

import { MARK_MESSAGE_AS_READ } from "~/actions";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED } from "~/getters";
import { DispositionNotificationMixin } from "~/mixins";
import { SET_BLOCK_REMOTE_IMAGES } from "~/mutations";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import MailViewerToolbar from "./MailViewerToolbar";
import MailViewerContent from "./MailViewerContent";

export default {
    name: "MailViewer",
    components: {
        BmExtension,
        MailViewerToolbar,
        MailViewerContent
    },
    mixins: [DispositionNotificationMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: {
                alert: { name: "mail.BLOCK_REMOTE_CONTENT", uid: "BLOCK_REMOTE_CONTENT", payload: this.message },
                options: { area: "right-panel", renderer: "BlockedRemoteContent" }
            }
        };
    },
    computed: {
        ...mapState("mail", {
            conversationByKey: ({ conversations }) => conversations.conversationByKey
        }),
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED }),
        ...mapState("mail", ["folders"]),
        trustRemoteContent() {
            return this.$store.state.settings.trust_every_remote_content !== "false";
        },
        remoteBlocked() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        conversation() {
            return this.conversationByKey[this.message.conversationRef?.key];
        }
    },
    watch: {
        "message.key": {
            handler: function () {
                this.REMOVE(this.alert.alert);
                this.SET_BLOCK_REMOTE_IMAGES(!this.trustRemoteContent);

                if (
                    !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                    this.folders[this.message.folderRef.key].writable
                ) {
                    this.MARK_MESSAGE_AS_READ([this.message]);
                }

                this.hideDispositionNotificationAlert();
                this.handleDispositionNotification([this.message]);
            },
            immediate: true
        }
    },
    destroyed() {
        this.REMOVE(this.alert.alert);
        this.hideDispositionNotificationAlert();
    },
    methods: {
        ...mapActions("mail", { MARK_MESSAGE_AS_READ }),
        ...mapActions("alert", { REMOVE, WARNING }),
        ...mapMutations("mail", { SET_BLOCK_REMOTE_IMAGES }),
        async setBlockRemote() {
            if (this.remoteBlocked) {
                const { total } = await apiAddressbooks.search(this.message.from.address);
                if (total === 0) {
                    this.WARNING(this.alert);
                } else {
                    this.SET_BLOCK_REMOTE_IMAGES(false);
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
.mail-viewer {
    z-index: 20;
    padding-top: $sp-6;
    padding-left: $sp-6 + $sp-5;
    padding-right: $sp-6;
    @include until-lg {
        padding-left: $sp-4;
        padding-right: $sp-4;
    }
}
</style>
