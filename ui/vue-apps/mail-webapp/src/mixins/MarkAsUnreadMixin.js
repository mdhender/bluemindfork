import ActionTextMixin from "./ActionTextMixin";
import FlagMixin from "./FlagMixin";

export default {
    mixins: [ActionTextMixin, FlagMixin],
    props: {
        conversation: {
            type: Object,
            default: undefined
        }
    },
    computed: {
        ariaText() {
            const fn = this.isMarkAsUnreadPressed ? this.markAsReadAriaText : this.markAsUnreadAriaText;
            return this.conversation ? fn(1, this.conversation.subject) : fn();
        }
    },
    methods: {
        action() {
            const fn = this.isMarkAsUnreadPressed ? this.markAsRead : this.markAsUnread;
            return this.conversation ? fn(this.conversation) : fn();
        }
    }
};
