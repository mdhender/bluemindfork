import SelectionMixin from "./SelectionMixin";

export default {
    mixins: [SelectionMixin],
    computed: {
        markAsReadText() {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_as_read", this.selected.length)
                : this.$tc("mail.actions.mark_as_read", this.selected.length);
        },
        markAsUnreadText() {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_as_unread", this.selected.length)
                : this.$tc("mail.actions.mark_as_unread", this.selected.length);
        },
        markAsFlaggedText() {
            return this.$tc("mail.actions.mark_flagged", this.selected.length);
        },
        markAsUnflaggedText() {
            return this.$tc("mail.actions.mark_unflagged", this.selected.length);
        },
        removeText() {
            return this.$tc("mail.actions.remove", this.selected.length);
        },
        moveText() {
            return this.$tc("mail.actions.move", this.selected.length);
        },
        selectionHasAtLeastOneConversation() {
            return this.conversationsActivated && this.selected.some(s => s.messages.length > 1);
        }
    },
    methods: {
        markAsReadAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_read.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.mark_read.aria", this.selected.length, { subject });
        },
        markAsUnreadAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_unread.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.mark_unread.aria", this.selected.length, { subject });
        },
        markAsFlaggedAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_flagged.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.mark_flagged.aria", this.selected.length, { subject });
        },
        markAsUnflaggedAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.mark_conversations_unflagged.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.mark_unflagged.aria", this.selected.length, { subject });
        },
        removeAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.remove.conversations.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.remove.aria", this.selected.length, { subject });
        },
        moveAriaText(subject) {
            return this.selectionHasAtLeastOneConversation
                ? this.$tc("mail.actions.move.conversations.aria", this.selected.length, { subject })
                : this.$tc("mail.actions.move.aria", this.selected.length, { subject });
        }
    }
};
