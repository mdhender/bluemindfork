import { mapGetters } from "vuex";
import SelectionMixin from "./SelectionMixin";
import { SELECTION_KEYS } from "~/getters";

export default {
    mixins: [SelectionMixin],
    computed: {
        ...mapGetters("mail", { $_ActionTextMixin_selectionKeys: SELECTION_KEYS }),
        $_ActionTextMixin_selectionLength() {
            return this.$_ActionTextMixin_selectionKeys.length;
        },
        markAsReadText() {
            return this.conversationsActivated
                ? this.$t("mail.actions.mark_conversations_as_read")
                : this.$tc("mail.actions.mark_as_read", this.$_ActionTextMixin_selectionLength);
        },
        markAsUnreadText() {
            return this.conversationsActivated
                ? this.$t("mail.actions.mark_conversations_as_unread")
                : this.$tc("mail.actions.mark_as_unread", this.$_ActionTextMixin_selectionLength);
        },
        markAsFlaggedText() {
            return this.$tc("mail.actions.mark_flagged", this.$_ActionTextMixin_selectionLength);
        },
        markAsUnflaggedText() {
            return this.$tc("mail.actions.mark_unflagged", this.$_ActionTextMixin_selectionLength);
        },
        removeText() {
            return this.$tc("mail.actions.remove", this.$_ActionTextMixin_selectionLength);
        },
        moveText() {
            return this.$tc("mail.actions.move", this.$_ActionTextMixin_selectionLength);
        }
    },
    methods: {
        markAsReadAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.mark_conversations_read.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.mark_read.aria", this.$_ActionTextMixin_selectionLength, { subject });
        },
        markAsUnreadAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.mark_conversations_unread.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.mark_unread.aria", this.$_ActionTextMixin_selectionLength, { subject });
        },
        markAsFlaggedAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.mark_conversations_flagged.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.mark_flagged.aria", this.$_ActionTextMixin_selectionLength, { subject });
        },
        markAsUnflaggedAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.mark_conversations_unflagged.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.mark_unflagged.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  });
        },
        removeAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.remove.conversations.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.remove.aria", this.$_ActionTextMixin_selectionLength, { subject });
        },
        moveAriaText(subject) {
            return this.conversationsActivated
                ? this.$tc("mail.actions.move.conversations.aria", this.$_ActionTextMixin_selectionLength, {
                      subject
                  })
                : this.$tc("mail.actions.move.aria", this.$_ActionTextMixin_selectionLength, { subject });
        }
    }
};
