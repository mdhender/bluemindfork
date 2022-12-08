<template>
    <component :is="entry.nameRenderer" v-if="entry.nameRenderer" class="pref-entry-name" :entry="entry" />
    <div v-else class="pref-entry-name fallback-pref-entry-name d-flex align-items-center">
        <span class="h3" :class="{ 'entry-disabled': entry.disabled }">{{ entry.name }}</span>
        <bm-label-icon v-if="entry.disabled" icon="exclamation-circle" class="text-warning ml-2">
            {{ $t("preferences.role.missing.warning") }}
        </bm-label-icon>
    </div>
</template>
<script>
import { BmLabelIcon } from "@bluemind/ui-components";

export default {
    name: "PrefEntryName",
    components: { BmLabelIcon },
    props: {
        entry: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.fallback-pref-entry-name {
    .entry-disabled {
        color: $neutral-fg-disabled;
    }
}
</style>
