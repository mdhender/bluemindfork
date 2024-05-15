<script>
import { BmIcon } from "@bluemind/ui-components";

export const priority = 10;

export default {
    name: "AttachmentIcon",
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
        if (this.message.hasAttachment) {
            return h("bm-icon", { props: { icon: "paperclip" } });
        }
        return this.next();
    }
};
</script>
