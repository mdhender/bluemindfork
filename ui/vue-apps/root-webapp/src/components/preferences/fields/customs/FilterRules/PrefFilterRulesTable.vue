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
                    :value="true"
                    :unchecked-value="false"
                    switch
                    @input="$emit('toggle-active', cell.item)"
                />
            </template>
            <template #cell(name)="cell">
                <div class="d-flex align-items-center">
                    <h2 class="d-flex">
                        <span class="mr-2" :class="{ 'text-alternate-light': !cell.item.active }">
                            {{ cell.value }}
                        </span>
                        <bm-badge v-if="cell.item.terminal" variant="secondary" pill>
                            {{ $t("preferences.mail.filters.terminal") }}
                        </bm-badge>
                    </h2>
                    <bm-button variant="inline-secondary" @click="cell.toggleDetails">
                        <bm-icon :icon="cell.detailsShowing ? 'chevron' : 'chevron-right'" size="lg" />
                    </bm-button>
                </div>
            </template>
            <template #cell(editable)="cell">
                <div v-if="cell.value" class="d-flex justify-content-end">
                    <bm-button variant="inline-secondary" @click="$emit('up', cell.item)">
                        <bm-icon icon="arrow-up" size="lg" />
                    </bm-button>
                    <bm-button variant="inline-secondary" @click="$emit('down', cell.item)">
                        <bm-icon icon="arrow-down" size="lg" />
                    </bm-button>
                    <bm-button variant="inline-secondary" @click="$emit('edit', cell.item)">
                        <bm-icon icon="pencil" size="lg" />
                    </bm-button>
                    <bm-button variant="inline-secondary" @click="remove(cell.item)">
                        <bm-icon icon="trash" size="lg" />
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
export default {
    name: "PrefFilterRulesTable",
    components: { BmBadge, BmButton, BmIcon, BmFormCheckbox, BmPagination, BmTable, PrefFilterRuleDetails },
    props: {
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
                { key: "active", label: "", thStyle: "width: 5%;", tdClass: "align-middle" },
                { key: "name", label: "", thStyle: "width: 60%;" },
                { key: "editable", label: "", thStyle: "width: 35%;" }
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
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                }
            );

            if (confirm) {
                this.$emit("remove", item);
            }
        }
    }
};
</script>
