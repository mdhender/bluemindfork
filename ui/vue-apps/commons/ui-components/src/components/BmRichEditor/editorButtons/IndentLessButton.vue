<script>
import { setIndentation } from "roosterjs-editor-api";
import { Indentation } from "roosterjs-editor-types";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "IndentLessButton",
    extends: CommandIconButton,
    data() {
        return {
            title: this.$t("styleguide.rich_editor.outdent.tooltip"),
            icon: "indent-less"
        };
    },
    methods: {
        action() {
            setIndentation(this.editor, Indentation.Decrease);
        }
    }
};
</script>
