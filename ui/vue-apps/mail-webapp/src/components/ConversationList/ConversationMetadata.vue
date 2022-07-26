<script>
import { CONVERSATION_METADATA } from "~/getters";

export default {
    name: "ConversationMetadata",
    props: {
        conversationKey: {
            type: [String, Number],
            required: true
        }
    },
    computed: {
        metadata() {
            return this.$store.getters[`mail/${CONVERSATION_METADATA}`](this.conversationKey);
        }
    },
    render() {
        return this.$scopedSlots.default({
            conversation: this.metadata
        });
    }
};
</script>
