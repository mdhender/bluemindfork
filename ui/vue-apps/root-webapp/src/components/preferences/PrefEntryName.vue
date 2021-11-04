<template>
    <component :is="entry.nameRenderer" v-if="entry.nameRenderer" :entry="entry" />
    <div v-else class="d-flex align-items-center">
        <span class="h2" :class="{ 'text-alternate-light': entry.disabled }">{{ entry.name }}</span>
        <bm-label-icon v-if="entry.disabled" icon="exclamation-circle" class="text-warning ml-2">
            {{ $t("preferences.role.missing.warning") }}
        </bm-label-icon>
    </div>
</template>
<script>
import { BmLabelIcon } from "@bluemind/styleguide";

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
