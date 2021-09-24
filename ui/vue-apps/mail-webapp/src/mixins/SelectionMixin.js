import { mapGetters, mapState } from "vuex";
import {
    CONVERSATION_MESSAGE_BY_KEY,
    CURRENT_CONVERSATION_METADATA,
    MY_SENT,
    SELECTION,
    SELECTION_KEYS
} from "~/getters";

export default {
    computed: {
        ...mapGetters("mail", {
            $_SelectionMixin_CONVERSATION_MESSAGE_BY_KEY: CONVERSATION_MESSAGE_BY_KEY,
            $_SelectionMixin_CURRENT_CONVERSATION_METADATA: CURRENT_CONVERSATION_METADATA,
            $_SelectionMixin_MY_SENT: MY_SENT,
            $_SelectionMixin_SELECTION: SELECTION
        }),
        ...mapState("mail", {
            $_SelectionMixin_currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        selected() {
            if (this.$_SelectionMixin_SELECTION.length > 0) {
                return this.$_SelectionMixin_SELECTION;
            } else if (this.$_SelectionMixin_currentConversation) {
                return [this.$_SelectionMixin_CURRENT_CONVERSATION_METADATA];
            }
            return [];
        },
        selectionLength() {
            const selectionLength = this.$store.getters["mail/" + SELECTION_KEYS].length;
            if (selectionLength > 0) {
                return selectionLength;
            }
            return this.$_SelectionMixin_CURRENT_CONVERSATION_METADATA ? 1 : 0;
        }
    }
};
