<template>
    <bm-icon-dropdown v-if="!hasMenuButtonSlot" v-bind="props" :variant="variant">
        <slot />
    </bm-icon-dropdown>
    <bm-dropdown v-else v-bind="props" variant="text">
        <template #button-content>
            <slot name="menu-button" />
        </template>
        <slot />
    </bm-dropdown>
</template>

<script setup>
import { provide, useSlots } from "vue";
import BmIconDropdown from "../dropdown/BmIconDropdown";
import BmDropdown from "../dropdown/BmDropdown";
import BmIcon from "../BmIcon";

const props = defineProps({
    icon: {
        type: String,
        default: "3dots-horizontal"
    },
    size: {
        type: String,
        default: "md"
    },
    noCaret: {
        type: Boolean,
        default: true
    },
    variant: {
        type: String,
        default: undefined
    }
});

provide("$context", "menu");

const slots = useSlots();
const hasMenuButtonSlot = slots?.["menu-button"];
</script>
