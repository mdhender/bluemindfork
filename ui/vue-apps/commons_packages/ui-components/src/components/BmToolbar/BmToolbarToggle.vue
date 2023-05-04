<script setup>
import { defineProps } from "vue";
import BmToggleableButton from "../buttons/BmToggleableButton";
import BmToggleableIconButton from "../buttons/BmToggleableIconButton";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";
import { useToolbarContext } from "./toolbar";

defineProps({
    pressed: {
        type: Boolean,
        default: false
    }
});

const { isInToolbar } = useToolbarContext();
</script>

<template>
    <div v-if="isInToolbar">
        <bm-toggleable-icon-button v-bind="$attrs" :pressed="pressed" v-on="$listeners" />
    </div>

    <bm-dropdown-item-toggle
        v-else
        v-bind="$attrs"
        :checked="pressed"
        v-on="$listeners"
        @change="$emit('update:pressed', $event)"
    >
        <slot></slot>
    </bm-dropdown-item-toggle>
</template>
