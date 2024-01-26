<script setup>
import { defineProps } from "vue";
import BmToolbarElement from "./BmToolbarElement";
import BmToggleableIconButton from "../buttons/BmToggleableIconButton";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";

defineProps({
    pressed: {
        type: Boolean,
        default: false
    },
    icon: {
        type: String,
        required: true
    },
    text: {
        type: String,
        required: true
    }
});
</script>

<template>
    <bm-toolbar-element>
        <template #toolbar>
            <bm-toggleable-icon-button v-bind="$attrs" :icon="icon" :pressed="pressed" v-on="$listeners">
                <slot />
            </bm-toggleable-icon-button>
        </template>
        <template #menu>
            <bm-dropdown-item-toggle
                v-bind="$attrs"
                :icon="icon"
                :text="text"
                :checked="pressed"
                v-on="$listeners"
                @change="$emit('update:pressed', $event)"
            >
                {{ text }}
            </bm-dropdown-item-toggle>
        </template>
    </bm-toolbar-element>
</template>
