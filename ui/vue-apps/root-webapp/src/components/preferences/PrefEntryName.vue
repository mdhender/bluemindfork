<template>
    <component :is="component.name" v-if="isComponent" v-bind="{ ...component.options }" :entry="entry" />
    <div v-else class="d-flex align-items-center">
        <span class="h2" :class="{ 'text-alternate-light': entry.disabled }">{{ entry.name }}</span>
        <bm-label-icon v-if="entry.disabled" icon="exclamation-circle" class="text-warning ml-2">
            {{ $t("preferences.role.missing.warning") }}
        </bm-label-icon>
    </div>
</template>
<script>
import { BmLabelIcon } from "@bluemind/styleguide";
import PrefSoonAvailable from "./PrefEntryName/PrefSoonAvailable";

export default {
    name: "PrefEntryName",
    components: { BmLabelIcon, PrefSoonAvailable },
    props: {
        entry: {
            type: Object,
            required: true
        }
    },
    computed: {
        isComponent() {
            return typeof this.entry.name === "object";
        },
        component() {
            return this.entry.name;
        }
    }
};
</script>
