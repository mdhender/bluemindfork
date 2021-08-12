import { mapGetters, mapState } from "vuex";
import { CONVERSATION_MESSAGE_BY_KEY, CURRENT_CONVERSATION_METADATA, MY_SENT, SELECTION } from "~/getters";

export default {
    computed: {
        ...mapGetters("mail", {
            $_SelectionMixin_CONVERSATION_MESSAGE_BY_KEY: CONVERSATION_MESSAGE_BY_KEY,
            $_SelectionMixin_currentConversationMetadata: CURRENT_CONVERSATION_METADATA,
            $_SelectionMixin_MY_SENT: MY_SENT,
            $_SelectionMixin_SELECTION: SELECTION
        }),
        ...mapState("session", { $_SelectionMixin_userSettings: ({ settings }) => settings.remote }),
        ...mapState("mail", {
            $_SelectionMixin_folders: "folders",
            $_SelectionMixin_activeFolder: "activeFolder",
            $_SelectionMixin_currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        selected() {
            if (this.$_SelectionMixin_SELECTION.length > 0) {
                return this.$_SelectionMixin_SELECTION;
            } else if (this.$_SelectionMixin_currentConversation) {
                return [this.$_SelectionMixin_currentConversationMetadata];
            }
            return [];
        },
        conversationsActivated() {
            return (
                this.$_SelectionMixin_userSettings.mail_thread === "true" &&
                this.$_SelectionMixin_folders[this.$_SelectionMixin_activeFolder].allowConversations
            );
        }
    }
};
