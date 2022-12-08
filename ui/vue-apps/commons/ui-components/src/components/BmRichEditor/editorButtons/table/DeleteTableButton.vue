<template>
    <bm-button variant="text" size="lg" class="px-5" icon="trash" @click="deleteTable">
        {{ $t("styleguide.rich_editor.table.delete_table") }}
    </bm-button>
</template>

<script>
import { editTable } from "roosterjs-editor-api";
import { TableOperation } from "roosterjs-editor-types";
import BmButton from "../../../buttons/BmButton";

export default {
    name: "DeleteTableButton",
    components: { BmButton },
    props: {
        editor: {
            type: Object,
            required: true
        }
    },
    methods: {
        deleteTable() {
            editTable(this.editor, TableOperation.DeleteTable);
        }
    }
};
</script>
