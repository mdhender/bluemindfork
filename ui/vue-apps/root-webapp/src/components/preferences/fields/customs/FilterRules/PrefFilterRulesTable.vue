<template>
    <div class="pref-filter-rules-table">
        <bm-form-input
            v-model="pattern"
            class="pref-filter mt-2 mb-3"
            :placeholder="$t('common.action.search')"
            icon="magnifier"
            resettable
            left-icon
            :aria-label="$t('common.action.search')"
            autocomplete="off"
            @reset="pattern = ''"
        />
        <bm-table
            :items="filteredFilters"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="index"
            details-td-class="bg-surface"
        >
            <template #cell(active)="cell">
                <bm-form-checkbox
                    :checked="cell.item.active"
                    :disabled="!editable"
                    :value="true"
                    :unchecked-value="false"
                    switch
                    @change="$emit('toggle-active', cell.item)"
                />
            </template>
            <template #cell(name)="cell">
                <div class="d-flex align-items-center mr-5" @click="cell.toggleDetails">
                    <bm-button-expand :expanded="cell.detailsShowing" />
                    <div class="d-flex overflow-hidden align-items-center flex-fill">
                        <div
                            v-highlight="{
                                pattern: highlight,
                                text: cell.value || $t('preferences.mail.filters.unnamed')
                            }"
                            class="bold filter-name mr-5 text-nowrap text-truncate"
                            :class="{ 'filter-inactive': !cell.item.active }"
                        >
                            {{ cell.value || $t("preferences.mail.filters.unnamed") }}
                        </div>
                        <div
                            v-highlight="{ pattern: highlight, text: cell.item.desc }"
                            class="filter-desc desktop-only caption text-truncate flex-fill text-right"
                        >
                            {{ cell.item.desc }}
                        </div>
                        <bm-badge v-if="cell.item.terminal" class="desktop-only ml-3 caption-bold" pill>
                            {{ $t("preferences.mail.filters.terminal") }}
                        </bm-badge>
                    </div>
                </div>
            </template>
            <template #cell(editable)="cell">
                <div v-if="editable && cell.value" class="actions">
                    <bm-icon-button
                        class="desktop-only"
                        variant="compact"
                        icon="pencil"
                        @click="$emit('edit', cell.item)"
                    />
                    <bm-icon-button class="desktop-only" variant="compact" icon="trash" @click="remove(cell.item)" />
                    <bm-icon-dropdown variant="compact" icon="3dots-horizontal" no-caret>
                        <bm-dropdown-item-button class="mobile-only" icon="pencil" @click="$emit('edit', cell.item)">
                            {{ $t("common.edit") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button class="mobile-only" icon="trash" @click="remove(cell.item)">
                            {{ $t("common.delete") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-divider class="mobile-only" />
                        <bm-dropdown-item-button icon="table-row-plus-up" @click="$emit('create-before', cell.item)">
                            {{ $t("preferences.mail.filters.create.before") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button icon="table-row-plus-down" @click="$emit('create-after', cell.item)">
                            {{ $t("preferences.mail.filters.create.after") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button
                            icon="arrow-up-bar"
                            :disabled="filterIndex(cell.item) === 0"
                            @click="$emit('top', cell.item)"
                        >
                            {{ $t("preferences.mail.filters.move.top") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button
                            icon="arrow-up"
                            :disabled="filterIndex(cell.item) === 0"
                            @click="$emit('up', { filter: cell.item, relativeTo: filteredFilters[cell.index - 1] })"
                        >
                            {{ $t("preferences.mail.filters.move.up") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button
                            icon="arrow-down"
                            :disabled="filterIndex(cell.item) === filters.length - 1"
                            @click="$emit('down', { filter: cell.item, relativeTo: filteredFilters[cell.index + 1] })"
                        >
                            {{ $t("preferences.mail.filters.move.down") }}
                        </bm-dropdown-item-button>
                        <bm-dropdown-item-button
                            icon="arrow-down-bar"
                            :disabled="filterIndex(cell.item) === filters.length - 1"
                            @click="$emit('bottom', cell.item)"
                        >
                            {{ $t("preferences.mail.filters.move.bottom") }}
                        </bm-dropdown-item-button>
                    </bm-icon-dropdown>
                </div>
            </template>
            <template #row-details="row">
                <pref-filter-rule-details :filter="row.item" />
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="filteredFilters.length" :per-page="perPage" />
    </div>
</template>

<script>
import debounce from "lodash.debounce";
import { normalize } from "@bluemind/string";
import {
    BmBadge,
    BmButtonExpand,
    BmDropdownDivider,
    BmDropdownItemButton,
    BmFormCheckbox,
    BmFormInput,
    BmIconButton,
    BmIconDropdown,
    BmPagination,
    BmTable,
    Highlight
} from "@bluemind/ui-components";
import PrefFilterRuleDetails from "./PrefFilterRuleDetails";
import { toString as filterToString } from "./filterRules";

export default {
    name: "PrefFilterRulesTable",
    components: {
        BmBadge,
        BmButtonExpand,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmFormCheckbox,
        BmFormInput,
        BmIconButton,
        BmIconDropdown,
        BmPagination,
        BmTable,
        PrefFilterRuleDetails
    },
    directives: { Highlight },
    props: {
        editable: { type: Boolean, required: true },
        filters: { type: Array, required: true },
        perPage: { type: Number, default: 10 }
    },
    data() {
        return {
            currentPage: 1,
            fields: [
                { key: "active", label: "", tdClass: "align-middle", class: "active-cell" },
                { key: "name", label: "", tdClass: "filter-info px-0", class: "name-cell" },
                { key: "editable", label: "", class: "actions-cell" }
            ],
            pattern: "",
            filteredFilters: [],
            highlight: undefined,
            filterFilters: debounce(() => {
                this.highlight = undefined;
                const filterFn = value => normalize(value).includes(normalize(this.pattern));
                this.filteredFilters = this.filters?.length
                    ? this.pattern
                        ? this.filters.filter(({ name, desc }) => filterFn(desc) || filterFn(name))
                        : this.filters
                    : [];
                this.highlight = this.pattern;
            }, 50)
        };
    },
    watch: {
        filters: {
            handler: function () {
                this.buildDescs();
                this.filterFilters();
            },
            immediate: true
        },
        pattern() {
            this.filterFilters();
        }
    },
    methods: {
        async remove(item) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.mail.filters.remove.desc", { name: item.label }),
                {
                    title: this.$t("preferences.mail.filters.remove", { name: item.label }),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel")
                }
            );

            if (confirm) {
                this.$emit("remove", item);
            }
        },
        buildDescs() {
            this.filters?.forEach(filter => (filter.desc = filterToString(filter, this.$i18n)));
        },
        filterIndex(filter) {
            return this.filters.findIndex(({ id }) => id === filter.id);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";

.pref-filter-rules-table {
    .b-table {
        max-width: base-px-to-rem(900);
        table-layout: fixed;

        thead {
            display: none;
        }
    }
    .active-cell {
        width: base-px-to-rem(65);
    }
    .name-cell {
        width: 100%;
        .filter-info {
            width: 100%;
            max-width: 0; // needed by sub elements with text-truncate class
            cursor: pointer;
        }
        .filter-name {
            flex: 1 0 auto;
            &.filter-inactive {
                color: $neutral-fg-disabled;
            }
        }
        .filter-desc {
            flex: 1 1 auto;
        }
        .bm-badge {
            height: base-px-to-rem(20);
        }
    }
    .actions-cell {
        width: base-px-to-rem(40);
        @include from-lg {
            width: base-px-to-rem(120);
        }
        .actions {
            display: flex;
            justify-content: end;
            gap: $sp-5;
        }
    }
}
</style>
