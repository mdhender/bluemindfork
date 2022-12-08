<template>
    <div class="bm-table-size-chooser text-center" @mouseout="resetDefaultSize">
        <div v-for="row in size" :key="row" class="d-flex">
            <div
                v-for="col in size"
                :key="col"
                :class="{ selected: row <= rowHovered && col <= colHovered, cell: true }"
                @mouseover="setSize(row, col)"
                @click="$emit('selected', { cols: colHovered, rows: rowHovered })"
            />
        </div>
        <div class="table-size-text">
            {{ text }}
        </div>
    </div>
</template>
<script>
export default {
    name: "BmFormTableSize",
    props: {
        size: {
            type: Number,
            default: 10
        }
    },
    data() {
        return {
            rowHovered: 1,
            colHovered: 1
        };
    },
    computed: {
        text() {
            return `${this.rowHovered} x ${this.colHovered}`;
        }
    },
    methods: {
        setSize(row, col) {
            this.rowHovered = row;
            this.colHovered = col;
        },
        resetDefaultSize() {
            this.rowHovered = 1;
            this.colHovered = 1;
        }
    }
};
</script>
<style lang="scss">
@import "../../css/_variables";
.bm-table-size-chooser {
    flex: none;
    .cell {
        height: 14px;
        width: 14px;
        margin: 0.5px;
    }
    .cell:not(.selected) {
        border: $neutral-fg-lo2 1px solid;
    }
    .selected {
        border: $secondary-fg 1px solid;
    }
    .table-size-text {
        padding-top: $sp-5;
    }
}
</style>
