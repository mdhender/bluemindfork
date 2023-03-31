<script>
import { BmIcon } from "@bluemind/ui-components";

export const priority = 20;

export default {
    name: "IcsIcon",
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
    render(h) {
        if (this.message.hasICS) {
            return h("bm-icon", { props: { icon: "calendar" } });
        }
        return this.next();
    }
};
</script>
