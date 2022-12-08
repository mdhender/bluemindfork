<template>
    <bm-icon-dropdown
        variant="compact"
        size="lg"
        icon="rich-editor-add-row"
        split
        :title="$t('styleguide.rich_editor.table.insert_row')"
        @click="insertBelowRow"
    >
        <bm-dropdown-item @click="insertAboveRow">
            <div>{{ $t("styleguide.rich_editor.table.insert_above_row") }}</div>
        </bm-dropdown-item>
        <bm-dropdown-item @click="insertBelowRow">
            <div>{{ $t("styleguide.rich_editor.table.insert_below_row") }}</div>
        </bm-dropdown-item>
    </bm-icon-dropdown>
</template>

<script>
import { editTable } from "roosterjs-editor-api";
import { TableOperation } from "roosterjs-editor-types";
import BmIconDropdown from "../../../dropdown/BmIconDropdown";
import BmDropdownItem from "../../../dropdown/BmDropdownItem";

export default {
    name: "InsertRowButton",
    components: { BmDropdownItem, BmIconDropdown },
    props: {
        editor: {
            type: Object,
            required: true
        }
    },
    methods: {
        insertBelowRow() {
            editTable(this.editor, TableOperation.InsertBelow);
        },
        insertAboveRow() {
            editTable(this.editor, TableOperation.InsertAbove);
        }
    }
};
</script>
