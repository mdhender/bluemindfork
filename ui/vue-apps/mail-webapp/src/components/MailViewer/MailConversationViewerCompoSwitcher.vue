<template>
    <mail-viewer-loading v-if="!MESSAGE_IS_LOADED(message.key)" />
    <mail-conversation-viewer-message v-else-if="!isDraft && !message.composing" v-bind="$props" v-on="$listeners" />
    <mail-conversation-viewer-draft-editor v-else-if="message.composing" v-bind="$props" v-on="$listeners" />
    <mail-conversation-viewer-draft v-else v-bind="$props" v-on="$listeners" />
</template>

<script>
import MailConversationViewerDraft from "./MailConversationViewer/MailConversationViewerDraft";
import MailConversationViewerDraftEditor from "./MailConversationViewer/MailConversationViewerDraftEditor";
import MailViewerLoading from "./MailViewerLoading";
import MailConversationViewerMessage from "./MailConversationViewer/MailConversationViewerMessage";
import MailConversationViewerItemMixin from "./MailConversationViewer/MailConversationViewerItemMixin";
import { MESSAGE_IS_LOADED } from "~/getters";
import { mapGetters } from "vuex";

export default {
    name: "MailConversationViewerCompoSwitcher",
    components: {
        MailConversationViewerDraft,
        MailConversationViewerDraftEditor,
        MailConversationViewerMessage,
        MailViewerLoading
    },
    mixins: [MailConversationViewerItemMixin],
    provide() {
        return {
            $messageViewerRoot: this
        };
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_IS_LOADED })
    }
};
</script>
