<template>
    <div class="pref-tags-subset">
        <span :class="{ disabled: tags.length === 0 }">{{ title }}</span>
        <bm-button variant="inline-secondary" size="lg" :disabled="tags.length === 0" @click="showTable_ = !showTable_">
            <bm-icon :icon="showTable ? 'chevron' : 'chevron-right'" />
        </bm-button>
        <pref-tags-table v-if="showTable" class="pref-item-width" :tags="tags" v-on="$listeners" />
        <div v-if="editable" class="d-flex justify-content-end pref-item-width">
            <bm-button variant="outline-secondary" @click="$emit('edit', {})">
                <bm-icon icon="plus" class="mr-1" />
                {{ $t("preferences.general.tags.create") }}
            </bm-button>
        </div>
    </div>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";
import PrefTagsTable from "./PrefTagsTable";

export default {
    name: "PrefTagsSubset",
    components: { BmButton, BmIcon, PrefTagsTable },
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
        color: $alternate-light;
    }
}
</style>
