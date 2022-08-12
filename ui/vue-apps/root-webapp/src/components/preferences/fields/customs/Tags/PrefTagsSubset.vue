<template>
    <div class="pref-tags-subset">
        <span :class="{ disabled: tags.length === 0 }">{{ title }}</span>

        <bm-button-expand :disabled="tags.length === 0" :expanded="showTable" @click="showTable_ = !showTable_" />
        <pref-tags-table v-if="showTable" class="pref-item-width" :tags="tags" v-on="$listeners" />
        <div v-if="editable" class="d-flex justify-content-end pref-item-width">
            <bm-button variant="outline" size="lg" icon="plus" @click="$emit('edit', {})">
                {{ $t("preferences.general.tags.create") }}
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmButtonExpand } from "@bluemind/styleguide";
import PrefTagsTable from "./PrefTagsTable";

export default {
    name: "PrefTagsSubset",
    components: { BmButton, BmButtonExpand, PrefTagsTable },
    props: {
        title: {
            type: String,
            default: ""
        },
        tags: {
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
            return this.tags.length > 0 && this.showTable_;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-tags-subset {
    .disabled {
        color: $neutral-fg-disabled;
    }
}
</style>
