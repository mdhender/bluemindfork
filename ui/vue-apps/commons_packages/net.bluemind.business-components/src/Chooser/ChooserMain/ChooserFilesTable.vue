<template>
    <div class="chooser-files-table">
        <bm-table
            ref="table"
            :items="items"
            :fields="fields_"
            selectable
            sort-by="type"
            :busy="busy"
            show-empty
            :per-page="perPage"
            :current-page="currentPage"
            :fill="false"
            @row-clicked="select"
        >
            <template #table-busy>
                <div class="text-center">
                    <bm-spinner />
                </div>
            </template>
            <template #empty>
                <slot name="empty" />
            </template>
            <template #head(selected)>
                <div class="icon-or-check-wrapper">
                    <bm-check
                        :indeterminate="underSelection && !allSelected"
                        :checked="allSelected"
                        @change="toggleSelection"
                    />
                </div>
            </template>
            <template #cell(selected)="{ item }">
                <template v-if="isDirectory(item)">
                    <div class="icon-or-check-wrapper">
                        <bm-icon icon="folder-fill" />
                    </div>
                </template>
                <template v-else-if="underSelection">
                    <div class="icon-or-check-wrapper">
                        <bm-check :checked="isFileSelected(item)" @change="select(item)" />
                    </div>
                </template>
                <template v-else>
                    <div class="icon-or-check-wrapper">
                        <bm-file-icon :file="item" />
                    </div>
                </template>
            </template>
            <template #cell(size)="{ item }">
                <span v-if="isDirectory(item)"> {{ $t("common.folder") }} </span>
                <span v-else>{{ displaySize(item.size) }} </span>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" class="ml-7 mb-5" :total-rows="items.length" :per-page="perPage" />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmCheck, BmIcon, BmFileIcon, BmPagination, BmTable, BmSpinner } from "@bluemind/ui-components";
import { computeUnit } from "@bluemind/file-utils";
import {
    ADD_SELECTED_FILE,
    REMOVE_SELECTED_FILE,
    RESET_SELECTED_FILES,
    SET_PATH,
    SET_SELECTED_FILES
} from "../store/mutations";

const ITEM_TYPES = {
    FILE: "FILE",
    DIRECTORY: "DIRECTORY"
};

export default {
    name: "ChooserFilesTable",
    components: { BmCheck, BmIcon, BmFileIcon, BmPagination, BmTable, BmSpinner },
    props: {
        items: {
            type: Array,
            required: true
        },
        displayFields: {
            type: Array,
            required: true
        },
        busy: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            table: null,
            fields: [
                { key: "selected", label: "", class: "select-column", tdClass: this.tdClassFn },
                {
                    key: "name",
                    label: this.$t("styleguide.table.name"),
                    class: "name-column",
                    sortable: true,
                    tdClass: this.tdClassFn
                },
                {
                    key: "size",
                    label: this.$t("common.file.size"),
                    class: "size-column",
                    sortable: true,
                    tdClass: this.tdClassFn
                },
                {
                    key: "path",
                    label: this.$t("common.path"),
                    class: "path-column",
                    sortable: true,
                    tdClass: this.tdClassFn
                }
            ],
            perPage: 100,
            currentPage: 1
        };
    },
    computed: {
        ...mapState("chooser", ["path", "selectedFiles"]),
        fields_() {
            return this.fields.filter(({ key }) => this.displayFields.includes(key));
        },
        underSelection() {
            return this.selectedFilesCount > 0;
        },
        allSelected() {
            return this.files.length && this.selectedFilesCount === this.files.length;
        },
        files() {
            return this.items.filter(({ type }) => type === ITEM_TYPES.FILE);
        },
        selectedFilesCount() {
            return this.selectedFiles.length;
        }
    },
    mounted() {
        this.table = this.$refs.table;
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        isDirectory(item) {
            return item.type === ITEM_TYPES.DIRECTORY;
        },
        select(item) {
            if (this.isDirectory(item)) {
                this.$store.commit(`chooser/${SET_PATH}`, item.path);
            } else if (this.isFileSelected(item)) {
                this.$store.commit(`chooser/${REMOVE_SELECTED_FILE}`, item);
            } else {
                this.$store.commit(`chooser/${ADD_SELECTED_FILE}`, item);
            }
        },
        toggleSelection() {
            if (this.allSelected) {
                this.$store.commit(`chooser/${RESET_SELECTED_FILES}`);
            } else {
                this.$store.commit(`chooser/${SET_SELECTED_FILES}`, this.files);
            }
        },
        isFileSelected(item) {
            return !!this.selectedFiles.find(({ path }) => path === item.path);
        },
        tdClassFn(value, key, item) {
            return this.isFileSelected(item) && !this.isDirectory(item) ? "selected" : "";
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";
@use "sass:math";

@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.chooser-files-table {
    background: $surface;
    display: flex;
    flex-direction: column;
    gap: $sp-5;

    .bm-table {
        margin: 0 !important;

        &,
        .table-active {
            background: none !important;
        }

        thead {
            position: sticky;
            top: 0;
            z-index: $zindex-sticky;
            background-color: $surface;
            box-shadow: inset 0 -1px 0 $neutral-fg-lo3; // border for sticky header
            > tr {
                border-bottom: none;
            }
        }

        tr > th {
            vertical-align: middle;
            background-color: $neutral-bg-lo1;
            @include until-lg {
                display: none;
            }
        }
        tr {
            border-bottom: 1px solid $neutral-fg-lo3;
        }
        td {
            vertical-align: middle;
            background-image: none !important;
        }

        &.table-hover {
            & > tbody {
                tr:hover {
                    background-color: $neutral-bg-lo1 !important;
                }
                tr.b-table-row-selected {
                    @include bold;
                    td {
                        background: none !important;
                    }
                    background-color: $secondary-bg-lo1 !important;
                    &:hover {
                        background-color: $secondary-bg !important;
                    }
                }
            }
        }
        tr.b-table-empty-row {
            border-bottom: none;
            background: none !important;
        }
        .select-column {
            text-align: right;
            vertical-align: middle;
            width: 5%;
        }
        .icon-or-check-wrapper {
            $icon-size: map-get($icon-sizes, "md");
            width: $icon-size;
            padding-right: $sp-6;
            height: $icon-size;
            position: relative;
            display: inline-block;
            text-align: left;
            .bm-file-icon {
                width: $icon-size;
            }
            .bm-check {
                position: absolute;
                top: base-px-to-rem(2);
                left: 0;
            }
        }
        th > .icon-or-check-wrapper {
            .bm-check {
                top: 0;
            }
        }
    }
}
</style>
