<script>
import { editTable } from "roosterjs-editor-api";
import { TableOperation } from "roosterjs-editor-types";
import CommandIconButton from "../CommandIconButton";

export default {
    name: "DeleteColumnButton",
    extends: CommandIconButton,
    data() {
        return {
            title: this.$t("styleguide.rich_editor.table.delete_column"),
            icon: "rich-editor-delete-column"
        };
    },
    methods: {
        action() {
            editTable(this.editor, TableOperation.DeleteColumn);
        }
    }
};
</script>
