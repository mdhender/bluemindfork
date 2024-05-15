<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandDropdownItemButton from "./CommandDropdownItemButton";

export default {
    name: "AlignLeftButton",
    extends: CommandDropdownItemButton,
    data() {
        return {
            text: this.$t("styleguide.rich_editor.align_left.tooltip"),
            icon: "align-left"
        };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Left);
        }
    }
};
</script>
