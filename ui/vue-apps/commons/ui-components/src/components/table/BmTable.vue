<template>
    <b-table
        v-bind="[$attrs, $props]"
        class="bm-table"
        :class="{ 'fixed-row-height': fixedRowHeight }"
        v-on="$listeners"
    >
        <template v-for="(index, name) in $scopedSlots" v-slot:[name]="data">
            <slot :name="name" v-bind="data" />
        </template>
        <template v-if="filler" v-slot:custom-foot>
            <b-tr v-for="index in filler.size" :key="index" :class="{ striped: (index + filler.padding) % 2 }">
                <b-td :colspan="filler.span" aria-hidden>&nbsp;</b-td>
            </b-tr>
        </template>
    </b-table>
</template>
<script>
import { BTable, BTr, BTd } from "bootstrap-vue";

export default {
    name: "BmTable",
    components: { BTable, BTr, BTd },
    extends: BTable,
    props: {
        hover: {
            type: Boolean,
            default: true
        },
        sortIconLeft: {
            type: Boolean,
            default: true
        },
        striped: {
            type: Boolean,
            default: false
        },
        items: {
            type: Array,
            required: true
        },
        fill: {
            type: Boolean,
            default: true
        },
        fixedRowHeight: {
            type: Boolean,
            default: true
        }
    },
    computed: {
        filler() {
            const size = this.perPage - (this.items.length % this.perPage);
            const isLastPage = Math.ceil(this.items.length / this.perPage) === this.currentPage;

            if (this.fill && isLastPage && size < this.perPage && this.currentPage > 1) {
                return {
                    size,
                    span: Object.keys(this.items[0]).filter(key => !key.startsWith("_")).length,
                    padding: this.items.length % this.perPage
                };
            }
            return false;
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_type";
@import "../../css/_variables";

table.bm-table {
    th,
    td {
        white-space: normal;
        border: none !important;
        padding: 0;
        padding-left: $sp-3;
        &:first-child {
            padding-left: $sp-5 + $sp-3;
        }
        &:last-child {
            padding-right: $sp-5;
        }
    }
    tr {
        border-bottom: $table-border-width solid $neutral-fg-lo2;
    }
    thead th {
        color: $neutral-fg;
        @extend %caption-bold;
        &.b-table-sort-icon-left {
            background-position: left 0 center !important;
            padding-left: base-px-to-rem(12) !important;
            &:first-child {
                background-position: left $sp-5 center !important;
                padding-left: base-px-to-rem(12) + $sp-5 !important;
            }
        }
    }

    &.table-striped tfoot .striped {
        background-color: $neutral-bg-lo1;
    }
    &.fixed-row-height {
        th {
            $inner-height: $line-height-small + 2 * ($sp-4 + $sp-1);
            height: calc(#{$inner-height} + #{$table-border-width});
        }
        td {
            $inner-height: $line-height-high + 2 * $sp-4;
            height: calc(#{$inner-height} + #{$table-border-width});
        }
        th,
        td {
            padding-top: $table-border-width;
            vertical-align: middle;
        }
    }
}
</style>
