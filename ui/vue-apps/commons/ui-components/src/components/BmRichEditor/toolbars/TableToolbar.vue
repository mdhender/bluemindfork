<template>
    <bm-button-toolbar
        class="bm-rich-editor-toolbar-base table-toolbar position-fixed mb-3 shadow-sm bg-surface"
        :style="`top: ${position.top}px; left: ${position.left}px;`"
        @click.native="setPosition"
    >
        <bm-button-group>
            <insert-column-button :editor="editor" />
            <delete-column-button :editor="editor" />
        </bm-button-group>
        <bm-button-group>
            <insert-row-button :editor="editor" />
            <delete-row-button :editor="editor" />
        </bm-button-group>
        <delete-table-button :editor="editor" />
    </bm-button-toolbar>
</template>

<script>
import BmButtonToolbar from "../../buttons/BmButtonToolbar";
import BmButtonGroup from "../../buttons/BmButtonGroup";
import InsertColumnButton from "../editorButtons/table/InsertColumnButton";
import DeleteTableButton from "../editorButtons/table/DeleteTableButton";
import InsertRowButton from "../editorButtons/table/InsertRowButton";
import DeleteRowButton from "../editorButtons/table/DeleteRowButton";
import DeleteColumnButton from "../editorButtons/table/DeleteColumnButton";

export default {
    name: "TableToolbar",
    components: {
        BmButtonToolbar,
        BmButtonGroup,
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
            const parentTablePosition = this.table.getBoundingClientRect();
            if (parentTablePosition) {
                this.position.left = (parentTablePosition.left + parentTablePosition.right) / 2 - this.width / 2;
                this.position.top = parentTablePosition.bottom;
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables.scss";

.table-toolbar {
    .dropdown .btn {
        padding-right: $sp-1;
        padding-left: $sp-1;
    }
    & > .btn-group {
        border-right: ($sp-1 * 0.25) $neutral-fg-lo3 solid;
    }
}
</style>
