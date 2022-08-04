<template>
    <div class="pref-filter-rules-table">
        <bm-table
            :items="filters"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="index"
            details-td-class="bg-surface"
        >
            <template #cell(active)="cell">
                <bm-form-checkbox
                    v-model="cell.item.active"
                    :disabled="!editable"
                    :value="true"
                    :unchecked-value="false"
                    switch
                    @input="$emit('toggle-active', cell.item)"
                />
            </template>
            <template #cell(name)="cell">
                <div class="d-flex align-items-center" @click="cell.toggleDetails">
                    <bm-button class="pl-0" variant="inline-neutral">
                        <bm-icon :icon="cell.detailsShowing ? 'chevron' : 'chevron-right'" size="xs" />
                    </bm-button>
                    <h2>
                        <span class="filter-name mr-2 text-nowrap" :class="{ 'filter-inactive': !cell.item.active }">
                            {{ cell.value || $t("preferences.mail.filters.unnamed") }}
                        </span>
                    </h2>
                    <span class="text-truncate flex-fill">{{ buildDesc(cell.item) }}</span>
                    <h2 v-if="cell.item.terminal">
                        <bm-badge class="ml-3" variant="neutral" pill>
                            {{ $t("preferences.mail.filters.terminal") }}
                        </bm-badge>
                    </h2>
                </div>
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="d-flex justify-content-end">
                    <bm-button variant="inline-neutral" @click="$emit('up', cell.item)">
                        <bm-icon icon="arrow-up" />
                    </bm-button>
                    <bm-button variant="inline-neutral" @click="$emit('down', cell.item)">
                        <bm-icon icon="arrow-down" />
                    </bm-button>
                    <bm-button variant="inline-neutral" @click="$emit('edit', cell.item)">
                        <bm-icon icon="pencil" />
                    </bm-button>
                    <bm-button variant="inline-neutral" @click="remove(cell.item)">
                        <bm-icon icon="trash" />
                    </bm-button>
                </div>
            </template>
            <template #row-details="row">
                <pref-filter-rule-details :filter="row.item" />
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="filters.length" :per-page="perPage" />
    </div>
</template>

<script>
import { BmBadge, BmButton, BmIcon, BmFormCheckbox, BmPagination, BmTable } from "@bluemind/styleguide";
import PrefFilterRuleDetails from "./PrefFilterRuleDetails";
import { toString as filterToString } from "./filterRules";

export default {
    name: "PrefFilterRulesTable",
    components: { BmBadge, BmButton, BmIcon, BmFormCheckbox, BmPagination, BmTable, PrefFilterRuleDetails },
    props: {
        editable: {
            type: Boolean,
            required: true
        },
        filters: {
            type: Array,
            required: true
        },
        perPage: {
            type: Number,
            default: 5
        }
    },
    data() {
        return {
            currentPage: 1,
            fields: [
                { key: "active", label: "", tdClass: "align-middle" },
                { key: "name", label: "", tdClass: "filter-info px-0" },
                { key: "editable", label: "" }
            ]
        };
    },
    methods: {
        async remove(item) {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("preferences.mail.filters.remove.desc", { name: item.label }),
                {
                    title: this.$t("preferences.mail.filters.remove", { name: item.label }),
                    okTitle: this.$t("common.delete"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "secondary",
                    cancelVariant: "simple-neutral",
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );

            if (confirm) {
                this.$emit("remove", item);
            }
        },
        buildDesc(filter) {
            return filterToString(filter, this.$i18n);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-filter-rules-table {
    .filter-info {
        width: 100%;
        max-width: 0; // needed by sub elements with text-truncate class
        cursor: pointer;
    }
    .filter-name.filter-inactive {
        color: $neutral-fg-disabled;
    }
}
</style>
