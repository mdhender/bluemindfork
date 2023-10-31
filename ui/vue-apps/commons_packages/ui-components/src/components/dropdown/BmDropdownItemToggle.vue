<template>
    <bm-dropdown-item-button
        v-bind="$props"
        :icon="checked ? 'check' : ''"
        v-on="$listeners"
        @click="$emit('change', !checked)"
    >
        <slot />
    </bm-dropdown-item-button>
</template>
<script>
import BmDropdownItemButton from "./BmDropdownItemButton";

export default {
    name: "BmDropdownItemToggle",
    components: { BmDropdownItemButton },
    extends: BmDropdownItemButton,
    model: {
        prop: "checked",
        event: "change"
    },
    props: {
        checked: {
            type: Boolean,
            default: false
        }
    }
};
</script>
