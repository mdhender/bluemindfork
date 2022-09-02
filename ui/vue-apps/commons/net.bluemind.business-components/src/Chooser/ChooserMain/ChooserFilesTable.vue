<template>
    <div class="chooser-files-table">
        <bm-table
            ref="table"
            class="mb-1"
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
            <template #cell(selected)="{item}">
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
                        <bm-icon :icon="getMatchingIcon(item.name)" />
                    </div>
                </template>
            </template>
            <template #cell(size)="{item}">
                <span v-if="isDirectory(item)"> {{ $t("common.folder") }} </span>
                <span v-else>{{ displaySize(item.size) }} </span>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" class="ml-7 mt-5" :total-rows="items.length" :per-page="perPage" />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmCheck, BmIcon, BmPagination, BmTable, BmSpinner } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";
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
    components: { BmCheck, BmIcon, BmPagination, BmTable, BmSpinner },
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
        getMatchingIcon(name) {
            const mime = MimeType.getFromFilename(name);
            return MimeType.matchingIcon(mime);
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

@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";

.chooser-files-table {
    .bm-table {
        tr > th {
            vertical-align: middle;
            background-color: $neutral-bg-lo1 !important;
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
        & > tbody > tr:not(.b-table-empty-row):hover > td {
            background-color: $neutral-bg-lo1 !important;
            &.selected {
                background-color: $secondary-bg-lo1 !important;
            }
        }
        tr.b-table-empty-row {
            border-bottom: none;
        }
        tr > td.selected {
            background-color: $secondary-bg !important;
            font-weight: $font-weight-bold;
        }
        tr > td:not(.selected) {
            background-color: $surface-bg !important;
        }
        .select-column {
            text-align: right;
            vertical-align: middle;
            width: 5%;
        }
        .icon-or-check-wrapper {
            $icon-size: map-get($icon-sizes, "md");
            width: $icon-size + $sp-6;
            height: $icon-size;
            position: relative;
            display: inline-block;
            text-align: left;
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
