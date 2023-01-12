<script>
import { BmIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { hasEncryptionHeader } from "../../lib/helper";

export default {
    components: { BmIcon },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        next: {
            type: Function,
            required: true
        }
    },
    computed: {
        hasEncryptionHeader() {
            return !!this.conversation.messages.find(messageKey => {
                const message = this.$store.state.mail.conversations.messages[messageKey];
                return hasEncryptionHeader(message.headers);
            });
        }
    },
    render(h) {
        const isUnread = messageUtils.isUnread(this.conversation);
        const icon = isUnread ? "lock-fill" : "lock";
        const className = isUnread ? "unread" : "";
        if (this.hasEncryptionHeader) {
            return h("bm-icon", {
                props: {
                    icon
                },
                class: ["smime-lock-icon", className]
            });
        } else {
            return this.next();
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";

.unread {
    color: $primary-fg-hi1;
}
</style>
