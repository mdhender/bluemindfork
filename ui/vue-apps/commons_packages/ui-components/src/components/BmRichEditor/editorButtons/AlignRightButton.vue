<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "AlignRightButton",
    extends: CommandIconButton,
    data() {
        return { title: this.$t("styleguide.rich_editor.align_right.tooltip"), icon: "rich-editor-align-right" };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Right);
        }
    }
};
</script>
