<template>
    <bm-toolbar-icon-dropdown
        class="table-button"
        variant="compact"
        size="lg"
        icon="table"
        :disabled="disabled"
        no-caret
        right
        :title="$t('styleguide.rich_editor.table.insert_table')"
    >
        <bm-dropdown-item variant="text">
            <bm-table-size-chooser @selected="insertTable" />
        </bm-dropdown-item>
    </bm-toolbar-icon-dropdown>
</template>

<script>
import { insertTable } from "roosterjs-editor-api";

import BmToolbarIconDropdown from "../../BmToolbar/BmToolbarIconDropdown";
import BmDropdownItem from "../../dropdown/BmDropdownItem";
import BmTableSizeChooser from "../BmTableSizeChooser";

export default {
    name: "TableButton",
    components: { BmToolbarIconDropdown, BmDropdownItem, BmTableSizeChooser },
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
@import "../../../css/utils/variables.scss";

.table-button.bm-toolbar-icon-dropdown {
    .dropdown-item {
        background: none !important;
        height: initial;
        padding: $sp-5;
        justify-content: center;

        .dropdown-item-content {
            flex: none;
        }
    }
}
</style>
