<script>
import { BmIcon } from "@bluemind/ui-components";
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
            return h("bm-icon", {
                props: { icon: "lock-fill", size: "xs" },
                class: ["smime-lock-icon"]
            });
        } else {
            return this.next();
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.smime-lock-icon {
    color: $primary-fg-hi1;
}
</style>
