<script>
import { toggleStrikethrough } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "StrikeThroughButton",
    extends: CommandIconButton,
    props: {
        isStrikeThrough: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { title: this.$t("styleguide.rich_editor.strikethrough.tooltip"), icon: "rich-editor-strikethrough" };
    },
    computed: {
        isActive() {
            return this.isStrikeThrough;
        }
    },
    methods: {
        action() {
            toggleStrikethrough(this.editor);
        }
    }
};
</script>
