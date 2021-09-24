import { mapGetters } from "vuex";
import { CONVERSATIONS_ACTIVATED, CURRENT_CONVERSATION_METADATA } from "~/getters";
import SelectionMixin from "./SelectionMixin";

export default {
    mixins: [SelectionMixin],
    computed: {
        ...mapGetters("mail", { $_AlertTextMixin_CONVERSATIONS_ACTIVATED: CONVERSATIONS_ACTIVATED }),
        $_ActionTextMixin_subject() {
            const currentConversation = this.$store.getters["mail/" + CURRENT_CONVERSATION_METADATA];
            return currentConversation ? currentConversation.subject : "";
        },
        markAsReadText() {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_as_read", this.selectionLength)
                : this.$tc("mail.actions.mark_as_read", this.selectionLength);
        },
        markAsUnreadText() {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_as_unread", this.selectionLength)
                : this.$tc("mail.actions.mark_as_unread", this.selectionLength);
        },
        markAsFlaggedText() {
            return this.$tc("mail.actions.mark_flagged", this.selectionLength);
        },
        markAsUnflaggedText() {
            return this.$tc("mail.actions.mark_unflagged", this.selectionLength);
        },
        removeText() {
            return this.$tc("mail.actions.remove", this.selectionLength);
        },
        moveText() {
            return this.$tc("mail.actions.move", this.selectionLength);
        }
    },
    methods: {
        markAsReadAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_read.aria", length, { subject })
                : this.$tc("mail.actions.mark_read.aria", length, { subject });
        },
        markAsUnreadAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_unread.aria", length, { subject })
                : this.$tc("mail.actions.mark_unread.aria", length, { subject });
        },
        markAsFlaggedAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_flagged.aria", length, { subject })
                : this.$tc("mail.actions.mark_flagged.aria", length, { subject });
        },
        markAsUnflaggedAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.mark_conversations_unflagged.aria", length, { subject })
                : this.$tc("mail.actions.mark_unflagged.aria", length, { subject });
        },
        removeAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.remove.conversations.aria", length, { subject })
                : this.$tc("mail.actions.remove.aria", length, { subject });
        },
        moveAriaText(length = this.selectionLength, subject = this.$_ActionTextMixin_subject) {
            return this.$_AlertTextMixin_CONVERSATIONS_ACTIVATED
                ? this.$tc("mail.actions.move.conversations.aria", length, { subject })
                : this.$tc("mail.actions.move.aria", length, { subject });
        }
    }
};
