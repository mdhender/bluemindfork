<template>
    <section class="mail-viewer d-flex flex-column flex-grow-1 bg-surface pt-2">
        <bm-extension id="webapp.mail" path="viewer.header" :message="message" />
        <mail-viewer-toolbar
            v-if="conversation"
            class="d-none d-lg-flex px-lg-5 justify-content-end"
            :message="message"
            :conversation="conversation"
        />
        <mail-viewer-content :message="message" />
        <mail-viewer-toolbar
            v-if="conversation"
            class="d-flex d-lg-none justify-content-around"
            :message="message"
            :conversation="conversation"
        />
    </section>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex";
import { BmExtension } from "@bluemind/extensions";

import MailViewerToolbar from "./MailViewerToolbar";
import { CONVERSATION_LIST_UNREAD_FILTER_ENABLED } from "~/getters";
import { MARK_MESSAGE_AS_READ } from "~/actions";
import MailViewerContent from "./MailViewerContent";

export default {
    name: "MailViewer",
    components: {
        BmExtension,
        MailViewerToolbar,
        MailViewerContent
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            conversationByKey: ({ conversations }) => conversations.conversationByKey
        }),
        ...mapGetters("mail", { CONVERSATION_LIST_UNREAD_FILTER_ENABLED }),
        ...mapState("mail", ["folders"]),
        conversation() {
            return this.conversationByKey[this.message.conversationRef?.key];
        }
    },
    watch: {
        "message.key": {
            handler: function () {
                if (
                    !this.CONVERSATION_LIST_UNREAD_FILTER_ENABLED &&
                    this.folders[this.message.folderRef.key].writable
                ) {
                    this.MARK_MESSAGE_AS_READ([this.message]);
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { MARK_MESSAGE_AS_READ })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-viewer {
    z-index: 20;
}
</style>
