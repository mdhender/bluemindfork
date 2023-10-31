<script>
import { toggleItalic } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "ItalicButton",
    extends: CommandIconButton,
    props: {
        isItalic: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.italic.tooltip"), icon: "rich-editor-italic" };
    },
    computed: {
        isActive() {
            return this.isItalic;
        }
    },
    methods: {
        action() {
            toggleItalic(this.editor);
        }
    }
};
</script>
