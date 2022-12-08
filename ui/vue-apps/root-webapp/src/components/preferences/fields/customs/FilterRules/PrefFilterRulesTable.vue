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
                    <bm-button-expand :expanded="cell.detailsShowing" />
                    <div class="d-flex align-items-baseline text-truncate flex-fill">
                        <div
                            class="bold filter-name mr-5 text-nowrap"
                            :class="{ 'filter-inactive': !cell.item.active }"
                        >
                            {{ cell.value || $t("preferences.mail.filters.unnamed") }}
                        </div>
                        <div class="caption text-truncate flex-fill">{{ buildDesc(cell.item) }}</div>
                        <bm-badge v-if="cell.item.terminal" class="mx-3 caption-bold" pill>
                            {{ $t("preferences.mail.filters.terminal") }}
                        </bm-badge>
                    </div>
                </div>
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="actions">
                    <bm-icon-button variant="compact" icon="arrow-up" @click="$emit('up', cell.item)" />
                    <bm-icon-button variant="compact" icon="arrow-down" @click="$emit('down', cell.item)" />
                    <bm-icon-button variant="compact" icon="pencil" @click="$emit('edit', cell.item)" />
                    <bm-icon-button variant="compact" icon="trash" @click="remove(cell.item)" />
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
import { BmBadge, BmButtonExpand, BmIconButton, BmFormCheckbox, BmPagination, BmTable } from "@bluemind/ui-components";
import PrefFilterRuleDetails from "./PrefFilterRuleDetails";
import { toString as filterToString } from "./filterRules";

export default {
    name: "PrefFilterRulesTable",
    components: { BmBadge, BmButtonExpand, BmIconButton, BmFormCheckbox, BmPagination, BmTable, PrefFilterRuleDetails },
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
                { key: "active", label: "", tdClass: "align-middle", class: "active-cell" },
                { key: "name", label: "", tdClass: "filter-info px-0", class: "name-cell" },
                { key: "editable", label: "", class: "actions-cell" }
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
                    okVariant: "fill-accent",
                    cancelVariant: "text",
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
@import "~@bluemind/ui-components/src/css/variables";

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
        .filter-name.filter-inactive {
            color: $neutral-fg-disabled;
        }
    }
    .actions-cell {
        width: base-px-to-rem(170);
        .actions {
            display: flex;
            justify-content: end;
            gap: $sp-5;
        }
    }
}
</style>
