<script>
import { PreviewMixin } from "~/mixins";
import NoPreview from "./NoPreview";
export default {
    name: "PreviewTooLarge",
    components: { NoPreview },
    mixins: [PreviewMixin],
    props: {
        next: {
            type: Function,
            required: true
        }
    },
    render(h) {
        if (this.isLarge) {
            return h("no-preview", {
                props: { icon: "weight", text: this.$t("mail.preview.nopreview.large") },
                class: "preview-too-large"
            });
        }
        return this.next();
    }
};
</script>
