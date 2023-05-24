<script>
import { setIndentation } from "roosterjs-editor-api";
import { Indentation } from "roosterjs-editor-types";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "IndentMoreButton",
    extends: CommandIconButton,
    data() {
        return {
            title: this.$t("styleguide.rich_editor.indent.tooltip"),
            icon: "indent-more"
        };
    },
    methods: {
        action() {
            setIndentation(this.editor, Indentation.Increase);
        }
    }
};
</script>
