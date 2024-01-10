<script setup>
import { defineProps } from "vue";
import BmToggleableIconButton from "../buttons/BmToggleableIconButton";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";
import { useToolbarContext } from "./toolbar";

defineProps({
    pressed: {
        type: Boolean,
        default: false
    },
    icon: {
        type: String,
        required: true
    },
    overflownText: {
        type: String,
        required: true
    }
});

const { isInToolbar } = useToolbarContext();
</script>

<template>
    <bm-toggleable-icon-button v-if="isInToolbar" v-bind="$attrs" :icon="icon" :pressed="pressed" v-on="$listeners">
        <slot />
    </bm-toggleable-icon-button>
    <bm-dropdown-item-toggle
        v-else
        v-bind="$attrs"
        :icon="icon"
        :text="overflownText"
        :checked="pressed"
        v-on="$listeners"
        @change="$emit('update:pressed', $event)"
    >
        {{ overflownText }}
    </bm-dropdown-item-toggle>
</template>
