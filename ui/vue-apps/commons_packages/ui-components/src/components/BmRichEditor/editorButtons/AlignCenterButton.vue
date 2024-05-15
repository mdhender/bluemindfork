<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandDropdownItemButton from "./CommandDropdownItemButton";

export default {
    name: "AlignCenterButton",
    extends: CommandDropdownItemButton,
    data() {
        return {
            text: this.$t("styleguide.rich_editor.align_center.tooltip"),
            icon: "align-center"
        };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Center);
        }
    }
};
</script>
