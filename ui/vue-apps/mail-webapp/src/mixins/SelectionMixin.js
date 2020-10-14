import { mapGetters, mapState } from "vuex";
import { CONVERSATION_METADATA, SELECTION } from "~/getters";

export default {
    computed: {
        ...mapGetters("mail", {
            $_SelectionMixin_metadata: CONVERSATION_METADATA,
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
        }
    }
};
