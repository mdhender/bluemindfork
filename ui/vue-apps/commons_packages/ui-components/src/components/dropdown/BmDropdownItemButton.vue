<template>
    <b-dropdown-item-button v-bind="$props" :aria-keyshortcuts="shortcut" v-on="$listeners">
        <bm-icon v-if="icon" :icon="icon" fixed-width />
        <slot v-else name="icon" />
        <div class="dropdown-item-content"><slot /></div>
        <span v-if="shortcut" class="shortcut" aria-hidden="true">{{ shortcut }}</span>
    </b-dropdown-item-button>
</template>
<script>
import { BDropdownItemButton } from "bootstrap-vue";
import BmIcon from "../BmIcon";

export default {
    name: "BmDropdownItemButton",
    components: { BmIcon, BDropdownItemButton },
    extends: BDropdownItemButton,
    props: {
        icon: {
            type: String,
            required: false,
            default: null
        },
        shortcut: { type: String, required: false, default: null }
    }
};
</script>
