<script>
import { PreviewMixin } from "~/mixins";
import NoPreview from "./NoPreview";
export default {
    name: "PreviewBlockedRemoteContent",
    components: { NoPreview },
    mixins: [PreviewMixin],
    props: {
        next: {
            type: Function,
            required: true
        }
    },
    render(h) {
        if (this.hasBlockedRemoteContent) {
            return h("no-preview", { props: { icon: "exclamation-circle" }, class: "preview-blocked-remote-content" });
        }
        return this.next();
    }
};
</script>
