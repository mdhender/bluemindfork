<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "AlignLeftButton",
    extends: CommandIconButton,
    data() {
        return { title: this.$t("styleguide.rich_editor.align_left.tooltip"), icon: "rich-editor-align-left" };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Left);
        }
    }
};
</script>
