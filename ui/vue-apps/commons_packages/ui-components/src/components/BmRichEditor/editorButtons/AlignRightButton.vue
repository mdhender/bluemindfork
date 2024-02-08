<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandDropdownItemButton from "./CommandDropdownItemButton";

export default {
    name: "AlignRightButton",
    extends: CommandDropdownItemButton,
    data() {
        return {
            text: this.$t("styleguide.rich_editor.align_right.tooltip"),
            icon: "rich-editor-align-right"
        };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Right);
        }
    }
};
</script>
