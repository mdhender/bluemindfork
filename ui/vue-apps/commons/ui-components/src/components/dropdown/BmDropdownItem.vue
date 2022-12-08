<template>
    <b-dropdown-item v-bind="[$attrs, $props]" :aria-keyshortcuts="shortcut" v-on="$listeners">
        <bm-icon v-if="icon" :icon="icon" fixed-width />
        <slot v-else name="icon" />
        <div class="dropdown-item-content"><slot /></div>
        <span v-if="shortcut" class="shortcut" aria-hidden="true">{{ shortcut }}</span>
    </b-dropdown-item>
</template>
<script>
import { BDropdownItem } from "bootstrap-vue";
import BmIcon from "../BmIcon";

export default {
    name: "BmDropdownItem",
    components: { BmIcon, BDropdownItem },
    extends: BDropdownItem,
    props: {
        icon: {
            type: String,
            required: false,
            default: null
        },
        // Specify a shortcut for this item. Must be used only when the item is a recurring task
        shortcut: { type: String, required: false, default: null }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.dropdown-item {
    .dropdown-item-content {
        margin-left: $sp-4;
    }
}
</style>
