<template>
    <div class="pref-filter-rules-subset">
        <span :class="{ disabled: filters.length === 0 }">{{ title }}</span>
        <bm-button
            variant="inline-neutral"
            size="lg"
            :disabled="filters.length === 0"
            @click="showTable_ = !showTable_"
        >
            <bm-icon :icon="showTable ? 'chevron' : 'chevron-right'" />
        </bm-button>
        <pref-filter-rules-table v-if="showTable" :filters="filters" :editable="editable" v-on="$listeners" />
        <div v-if="editable" class="d-flex justify-content-end">
            <bm-button variant="outline-neutral" @click="onEdit">
                <bm-icon icon="plus" class="mr-1" />{{ $t("preferences.mail.filters.create") }}
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import PrefFilterRulesTable from "./PrefFilterRulesTable";

export default {
    name: "PrefFilterRulesSubset",
    components: { BmButton, BmIcon, PrefFilterRulesTable },
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
@import "~@bluemind/styleguide/css/_variables";

.pref-filter-rules-subset {
    .disabled {
        color: $neutral-fg-disabled;
    }
}
</style>
