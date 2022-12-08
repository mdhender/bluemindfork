<script>
import { toggleBold } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "BoldButton",
    extends: CommandIconButton,
    props: {
        isBold: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.bold.tooltip"), icon: "rich-editor-bold" };
    },
    computed: {
        isActive() {
            return this.isBold;
        }
    },
    methods: {
        action() {
            toggleBold(this.editor);
        }
    }
};
</script>
