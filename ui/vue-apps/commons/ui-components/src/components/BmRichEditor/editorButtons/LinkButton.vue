<script>
import CommandIconButton from "./CommandIconButton";

export default {
    name: "LinkButton",
    extends: CommandIconButton,
    props: {
        isLink: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.link.tooltip"), icon: "rich-editor-link" };
    },
    computed: {
        isActive() {
            return this.isLink;
        }
    },
    methods: {
        action() {
            this.$emit("open-link-modal");
        }
    }
};
</script>
