<template>
    <bm-toolbar
        class="bm-rich-editor-toolbar-base table-toolbar position-fixed mb-3 shadow-sm"
        :style="`top: ${position.top}px; left: ${position.left}px;`"
        @click.native="setPosition"
    >
        <insert-column-button :editor="editor" />
        <delete-column-button :editor="editor" />
        <bm-toolbar-divider />
        <insert-row-button :editor="editor" />
        <delete-row-button :editor="editor" />
        <bm-toolbar-divider />
        <delete-table-button :editor="editor" />
    </bm-toolbar>
</template>

<script>
import BmToolbar from "../../BmToolbar/BmToolbar";
import BmToolbarDivider from "../../BmToolbar/BmToolbarDivider";
import InsertColumnButton from "../editorButtons/table/InsertColumnButton";
import DeleteTableButton from "../editorButtons/table/DeleteTableButton";
import InsertRowButton from "../editorButtons/table/InsertRowButton";
import DeleteRowButton from "../editorButtons/table/DeleteRowButton";
import DeleteColumnButton from "../editorButtons/table/DeleteColumnButton";

export default {
    name: "TableToolbar",
    components: {
        BmToolbar,
        BmToolbarDivider,
        InsertColumnButton,
        DeleteTableButton,
        InsertRowButton,
        DeleteRowButton,
        DeleteColumnButton
    },
    props: {
        editor: {
            type: Object,
            required: true
        },
        table: {
            type: Node,
            required: true
        }
    },
    data() {
        return { width: null, position: { left: null, top: null } };
    },
    computed: {
        tableRect() {
            return this.table.getBoundingClientRect();
        }
    },
    watch: {
        table() {
            this.width = this.$el.offsetWidth;
            this.setPosition();
        }
    },
    mounted() {
        this.width = this.$el.offsetWidth;
        this.setPosition();
    },
    methods: {
        setPosition() {
            if (this.tableRect) {
                const toolbarLeft = (this.tableRect.left + this.tableRect.right) / 2 - this.width / 2;
                this.position.left = toolbarLeft < this.tableRect.left ? this.tableRect.left : toolbarLeft;
                this.position.top = this.tableRect.bottom;
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/utils/variables.scss";

.table-toolbar {
    background-color: $surface-hi1;

    .dropdown .btn {
        padding-right: $sp-1;
        padding-left: $sp-1;
    }
}
</style>
