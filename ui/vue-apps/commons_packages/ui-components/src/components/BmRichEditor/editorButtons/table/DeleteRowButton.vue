<script>
import { editTable } from "roosterjs-editor-api";
import { TableOperation } from "roosterjs-editor-types";
import CommandIconButton from "../CommandIconButton";

export default {
    name: "DeleteRowButton",
    extends: CommandIconButton,
    data() {
        return {
            title: this.$t("styleguide.rich_editor.table.delete_row"),
            icon: "rich-editor-delete-row"
        };
    },
    methods: {
        action() {
            editTable(this.editor, TableOperation.DeleteRow);
        }
    }
};
</script>
