<template>
    <bm-internal-table
        ref="bTable"
        v-bind="[$attrs, $props]"
        class="bm-table"
        :class="{ 'fixed-row-height': fixedRowHeight }"
        :sort-by.sync="curSortBy"
        :sort-desc.sync="curSortDesc"
        :tab-nav="tabNav"
        v-on="listeners"
        @row-selected="onRowSelected"
        @context-changed="doSelectRows"
        @input="doSelectRows"
    >
        <template v-for="field in fields" #[`head(${field.key})`]="data">
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
        <template v-for="(index, name) in $scopedSlots" #[name]="data">
            <slot :name="name" v-bind="data" />
        </template>
        <template v-if="filler" #custom-foot>
            <b-tr v-for="index in filler.size" :key="index" :class="{ striped: (index + filler.padding) % 2 }">
                <b-td aria-hidden>&nbsp;</b-td>
            </b-tr>
        </template>
    </bm-internal-table>
</template>

<script>
import { BTable, BTr, BTd } from "bootstrap-vue";
import BmSortControl from "../BmSortControl";
import BmInternalTable from "./BmInternalTable";

export default {
    name: "BmTable",
    components: { BmSortControl, BmInternalTable, /* BTable ,*/ BTr, BTd },
    extends: BTable,
    props: {
        hover: { type: Boolean, default: true },
        sortIconLeft: { type: Boolean, default: true },
        striped: { type: Boolean, default: false },
        items: { type: Array, required: true },
        fill: { type: Boolean, default: true },
        fixedRowHeight: { type: Boolean, default: true },
        selected: { type: Array, default: () => [] },
        tabNav: { type: Boolean, default: false }
    },
    data() {
        return {
            curSortBy: this.sortBy,
            curSortDesc: this.sortDesc,
            selectedPerPage: {},
            displayedItems: undefined
        };
    },
    computed: {
        listeners() {
            return Object.entries(this.$listeners).reduce((listeners, [name, value]) => {
                if (name !== "row-selected") {
                    listeners[name] = value;
                }
                return listeners;
            }, {});
        },
        filler() {
            const size = this.perPage - (this.items.length % this.perPage);
            const isLastPage = Math.ceil(this.items.length / this.perPage) === this.currentPage;

            if (this.fill && isLastPage && size < this.perPage && this.currentPage > 1) {
                return {
                    size,
                    padding: this.items.length % this.perPage
                };
            }
            return false;
        }
    },
    watch: {
        items() {
            this.selectedPerPage = {};
        },
        selected: {
            handler: function () {
                this.doSelectRows();
            },
            immediate: true
        }
    },
    mounted() {
        this.$watch(
            () => this.$refs.bTable.computedItems,
            value => (this.displayedItems = value)
        );
    },
    methods: {
        sortControlClicked(field) {
            if (this.curSortBy === field) {
                this.curSortDesc = !this.curSortDesc;
            } else {
                this.curSortBy = field;
                this.curSortDesc = false;
            }
        },
        selectRow(page, indexInPage, index) {
            if (page !== this.currentPage) {
                if (!this.selectedPerPage[page]) {
                    this.selectedPerPage[page] = [];
                }
                this.selectedPerPage[page][indexInPage] = this.items[index];
            } else {
                this.$refs.bTable.selectRow(indexInPage);
            }
        },
        unselectRow(page, indexInPage) {
            if (page !== this.currentPage) {
                if (!this.selectedPerPage[page]) {
                    this.selectedPerPage[page] = [];
                }
                this.selectedPerPage[page][indexInPage] = undefined;
            } else {
                this.$refs.bTable.unselectRow(indexInPage);
            }
        },
        onRowSelected(items) {
            if (this.isClear()) {
                this.$emit("clearing-selection");
            } else {
                const itemsCopy = [...items];
                this.selectedPerPage[this.currentPage] = this.$refs.bTable.selectedRows.map(isSelected =>
                    isSelected ? itemsCopy.shift() : undefined
                );
                this.$listeners["row-selected"]?.call(
                    this,
                    Object.values(this.selectedPerPage)
                        .flatMap(s => s)
                        .filter(Boolean)
                );
            }
        },
        isClear() {
            return this.displayedItems && this.displayedItems !== this.$refs.bTable.computedItems;
        },
        doSelectRow(itemIndexInList, doSelect) {
            const page = Math.ceil((itemIndexInList + 1) / this.perPage);
            const indexInPage = itemIndexInList - (page - 1) * this.perPage;
            const isSelected = this.selectedPerPage?.[page]?.[indexInPage];

            if (doSelect) {
                this.selectRow(page, indexInPage, itemIndexInList);
            } else if (!doSelect && isSelected) {
                this.unselectRow(page, indexInPage);
            }
        },
        async doSelectRows() {
            await this.$waitFor(() => this.$refs.bTable, Boolean);
            this.items.forEach((item, index) => {
                this.doSelectRow(index, this.selected.findIndex(selected => selected.uid === item.uid) !== -1);
            });
        },
        /** FIXME: WaitFor mixin exists in MailApp, should be common. */
        $waitFor(subject, opt_assert, options = { immediate: true, deep: true }) {
            let resolver;
            const assert = opt_assert || Boolean;
            const promise = new Promise(resolve => (resolver = resolve));
            const unwatch = this.$watch(subject, value => assert(value) && resolver(), options);
            promise.then(unwatch);
            return promise;
        }
    }
};
</script>
