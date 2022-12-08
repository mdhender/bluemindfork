<template>
    <div class="pref-filter-rules-subset">
        <span :class="{ disabled: filters.length === 0 }">{{ title }}</span>
        <bm-button-expand :expanded="showTable" :disabled="filters.length === 0" @click="showTable_ = !showTable_" />
        <pref-filter-rules-table v-if="showTable" :filters="filters" :editable="editable" v-on="$listeners" />
        <div>
            <bm-button v-if="editable" variant="outline" size="lg" icon="plus" @click="onEdit">
                {{ $t("preferences.mail.filters.create") }}
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmButtonExpand } from "@bluemind/ui-components";
import PrefFilterRulesTable from "./PrefFilterRulesTable";

export default {
    name: "PrefFilterRulesSubset",
    components: { BmButton, BmButtonExpand, PrefFilterRulesTable },
    props: {
        title: {
            type: String,
            default: ""
        },
        filters: {
            type: Array,
            required: true
        },
        editable: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return { showTable_: true };
    },
    computed: {
        showTable() {
            return this.filters.length > 0 && this.showTable_;
        },
        editableFilters() {
            return this.filters.map(f => ({ ...f, editable: this.editable }));
        }
    },
    methods: {
        onEdit() {
            this.$emit("edit", { criteria: [], actions: [], active: true, editable: true });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-filter-rules-subset {
    .disabled {
        color: $neutral-fg-disabled;
    }
}
</style>
