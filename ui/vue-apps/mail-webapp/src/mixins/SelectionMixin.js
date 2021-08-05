import { mapGetters, mapState } from "vuex";
import { CONVERSATION_MESSAGE_BY_KEY, CONVERSATION_METADATA, MY_SENT, SELECTION } from "~/getters";
import { removeSentDuplicates } from "~/model/conversations";

export default {
    computed: {
        ...mapGetters("mail", {
            $_SelectionMixin_CONVERSATION_MESSAGE_BY_KEY: CONVERSATION_MESSAGE_BY_KEY,
            $_SelectionMixin_metadata: CONVERSATION_METADATA,
            $_SelectionMixin_MY_SENT: MY_SENT,
            $_SelectionMixin_SELECTION: SELECTION
        }),
        ...mapState("mail", {
            $_SelectionMixin_folders: "folders",
            $_SelectionMixin_currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        selected() {
            if (this.$_SelectionMixin_SELECTION.length > 0) {
                return this.$_SelectionMixin_SELECTION.map(selected => this.$_SelectionMixin_metadata(selected.key));
            } else if (this.$_SelectionMixin_currentConversation) {
                const metadata = this.$_SelectionMixin_metadata(this.$_SelectionMixin_currentConversation.key);
                return metadata ? [metadata] : [];
            }
            return [];
        },
        selectionHasReadOnlyFolders() {
            return (
                this.selected &&
                this.selected.some(({ folderRef }) => !this.$_SelectionMixin_folders[folderRef.key].writable)
            );
        },
        selectedAreAllConversations() {
            return this.selected.every(
                s =>
                    removeSentDuplicates(
                        this.$_SelectionMixin_CONVERSATION_MESSAGE_BY_KEY(s.key),
                        this.$_SelectionMixin_MY_SENT
                    ).length > 1
            );
        },
        conversationsInSelection() {
            return this.selected.filter(
                s =>
                    removeSentDuplicates(
                        this.$_SelectionMixin_CONVERSATION_MESSAGE_BY_KEY(s.key),
                        this.$_SelectionMixin_MY_SENT
                    ).length > 1
            );
        }
    }
};
