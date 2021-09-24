export default {
    props: {
        index: {
            type: Number,
            required: true
        },
        conversation: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        },
        expandedMessages: {
            type: Array,
            required: true
        },
        nextIsHidden: {
            type: Boolean,
            required: true
        },
        isLastBeforeDraft: {
            type: Boolean,
            required: true
        },
        nextIsDraft: {
            type: Boolean,
            required: true
        },
        conversationSize: {
            type: Number,
            default: 0
        }
    },
    computed: {
        maxIndex() {
            return this.conversationSize - 1;
        },
        isMessageExpanded() {
            return Boolean(this.expandedMessages[this.index]);
        }
    }
};
