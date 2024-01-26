<script setup>
import { defineProps } from "vue";
import BmToggleableButton from "../buttons/BmToggleableButton";
import BmToolbarElement from "./BmToolbarElement";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";

defineProps({
    pressed: {
        type: Boolean,
        default: false
    }
});
</script>

<template>
    <bm-toolbar-element>
        <template #toolbar>
            <bm-toggleable-button v-bind="$attrs" :pressed="pressed" v-on="$listeners">
                <slot />
            </bm-toggleable-button>
        </template>
        <template #menu>
            <bm-dropdown-item-toggle
                v-bind="$attrs"
                :checked="pressed"
                v-on="$listeners"
                @change="$emit('update:pressed', $event)"
            >
                <slot />
            </bm-dropdown-item-toggle>
        </template>
    </bm-toolbar-element>
</template>
