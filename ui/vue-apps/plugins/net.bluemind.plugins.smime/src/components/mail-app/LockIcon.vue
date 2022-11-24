<script>
import { BmIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { isEncrypted } from "../../lib/helper";

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
        isEncrypted() {
            return !!this.conversation.messages.find(messageKey => {
                const message = this.$store.state.mail.conversations.messages[messageKey];
                return isEncrypted(message.headers);
            });
        }
    },
    render(h) {
        const isUnread = messageUtils.isUnread(this.conversation);
        const icon = isUnread ? "lock-fill" : "lock";
        const className = isUnread ? "unread" : "";
        if (this.isEncrypted) {
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
@import "@bluemind/ui-components/src/css/_variables.scss";

.unread {
    color: $primary-fg-hi1;
}
</style>
