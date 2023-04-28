<template>
    <bm-toggleable-button v-if="context === 'toolbar'" v-bind="$attrs" :pressed="pressed" v-on="$listeners"
        ><slot></slot
    ></bm-toggleable-button>
    <bm-dropdown-item-toggle
        v-else
        v-bind="$attrs"
        :checked="pressed"
        v-on="$listeners"
        @change="$emit('update:pressed', $event)"
        ><slot></slot
    ></bm-dropdown-item-toggle>
</template>
<script>
import BmToggleableButton from "../buttons/BmToggleableButton";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";
export default {
    name: "BmToolbarToggle",
    components: { BmToggleableButton, BmDropdownItemToggle },
    props: {
        pressed: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        context() {
            return this.$parent.$options.name === "BButtonToolbar" ? "toolbar" : "menu";
        }
    }
};
</script>

BmButton
