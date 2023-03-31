<script>
import { BmIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { hasEncryptionHeader } from "../../lib/helper";

export default {
    components: { BmIcon },
    props: {
        message: {
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
            return hasEncryptionHeader(this.message.headers);
        }
    },
    render(h) {
        if (this.hasEncryptionHeader) {
            const isUnread = messageUtils.isUnread(this.message);
            const icon = isUnread ? "lock-fill" : "lock";
            const className = isUnread ? "unread" : "";
            return h("bm-icon", {
                props: { icon },
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

.smime-lock-icon.unread {
    color: $primary-fg-hi1;
}
</style>
