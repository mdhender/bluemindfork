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
        isDraft: {
            type: Boolean,
            required: false,
            default: false
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
        }
    }
};
