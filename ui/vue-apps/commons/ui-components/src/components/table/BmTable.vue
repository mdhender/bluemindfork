<template>
    <b-table
        v-bind="[$attrs, $props]"
        class="bm-table"
        :class="{ 'fixed-row-height': fixedRowHeight }"
        :sort-by.sync="curSortBy"
        :sort-desc.sync="curSortDesc"
        v-on="$listeners"
    >
        <template v-for="field in fields" v-slot:[`head(${field.key})`]="data">
            <bm-sort-control
                v-if="data.field.sortable"
                :key="field.key"
                in-table
                :value="curSortBy === data.field.key ? (curSortDesc ? 'desc' : 'asc') : null"
                @click="sortControlClicked(data.field.key)"
            >
                {{ data.label }}
            </bm-sort-control>
        </template>
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
import BmSortControl from "../BmSortControl";
import { BTable, BTr, BTd } from "bootstrap-vue";

export default {
    name: "BmTable",
    components: { BmSortControl, BTable, BTr, BTd },
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
    data() {
        return {
            curSortBy: this.sortBy,
            curSortDesc: this.sortDesc
        };
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
    },
    methods: {
        sortControlClicked(field) {
            if (this.curSortBy === field) {
                this.curSortDesc = !this.curSortDesc;
            } else {
                this.curSortBy = field;
                this.curSortDesc = false;
            }
        }
    }
};
</script>
