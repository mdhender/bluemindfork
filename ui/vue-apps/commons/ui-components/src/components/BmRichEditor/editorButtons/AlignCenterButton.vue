<script>
import { Alignment } from "roosterjs-editor-types";
import { setAlignment } from "roosterjs-editor-api";
import CommandIconButton from "./CommandIconButton";

export default {
    name: "AlignCenterButton",
    extends: CommandIconButton,
    data() {
        return { title: this.$t("styleguide.rich_editor.align_center.tooltip"), icon: "rich-editor-align-center" };
    },
    methods: {
        action() {
            setAlignment(this.editor, Alignment.Center);
        }
    }
};
</script>
