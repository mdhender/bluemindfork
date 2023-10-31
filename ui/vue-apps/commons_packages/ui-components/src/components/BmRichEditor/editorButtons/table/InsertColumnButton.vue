<template>
    <bm-icon-dropdown
        variant="compact"
        size="lg"
        icon="rich-editor-add-column"
        split
        :title="$t('styleguide.rich_editor.table.insert_column')"
        @click="insertRightColumn"
    >
        <bm-dropdown-item @click="insertLeftColumn">
            <div>{{ $t("styleguide.rich_editor.table.insert_left_column") }}</div>
        </bm-dropdown-item>
        <bm-dropdown-item @click="insertRightColumn">
            <div>{{ $t("styleguide.rich_editor.table.insert_right_column") }}</div>
        </bm-dropdown-item>
    </bm-icon-dropdown>
</template>

<script>
import { editTable } from "roosterjs-editor-api";
import { TableOperation } from "roosterjs-editor-types";
import BmIconDropdown from "../../../dropdown/BmIconDropdown";
import BmDropdownItem from "../../../dropdown/BmDropdownItem";

export default {
    name: "InsertColumnButton",
    components: { BmDropdownItem, BmIconDropdown },
    props: {
        editor: {
            type: Object,
            required: true
        }
    },
    methods: {
        insertLeftColumn() {
            editTable(this.editor, TableOperation.InsertLeft);
        },
        insertRightColumn() {
            editTable(this.editor, TableOperation.InsertRight);
        }
    }
};
</script>
