<template>
    <bm-icon-dropdown
        class="table-button"
        variant="compact"
        size="lg"
        icon="rich-editor-table"
        :disabled="disabled"
        no-caret
        right
        :title="$t('styleguide.rich_editor.table.insert_table')"
    >
        <bm-dropdown-item>
            <bm-table-size-chooser @selected="insertTable" />
        </bm-dropdown-item>
    </bm-icon-dropdown>
</template>

<script>
import { insertTable } from "roosterjs-editor-api";

import BmIconDropdown from "../../dropdown/BmIconDropdown";
import BmDropdownItem from "../../dropdown/BmDropdownItem";
import BmTableSizeChooser from "../BmTableSizeChooser";

export default {
    name: "TableButton",
    components: { BmIconDropdown, BmDropdownItem, BmTableSizeChooser },
    props: {
        disabled: {
            type: Boolean,
            required: true
        },
        editor: {
            type: Object,
            required: true
        }
    },
    methods: {
        insertTable({ cols, rows }) {
            insertTable(this.editor, cols, rows);
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables.scss";

.table-button {
    .dropdown-item {
        background-color: $surface !important;
        height: initial;
        padding: $sp-5;
        justify-content: center;

        .dropdown-item-content {
            flex: none;
            margin: 0;
        }
    }
}
</style>
