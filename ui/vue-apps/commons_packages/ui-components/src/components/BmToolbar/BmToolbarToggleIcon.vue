<script>
import { defineProps, useAttrs } from "vue";
import BmToolbarElement from "./BmToolbarElement";
import BmToggleableIconButton from "../buttons/BmToggleableIconButton";
import BmDropdownItemToggle from "../dropdown/BmDropdownItemToggle";
export default {
    components: { BmDropdownItemToggle, BmToggleableIconButton, BmToolbarElement },
    inheritAttrs: false,
    props: {
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
            default: undefined
        }
    },
    computed: {
        text_() {
            return this.text || this.$attrs.title;
        }
    }
};
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
                :checked="pressed"
                v-on="$listeners"
                @change="$emit('update:pressed', $event)"
            >
                {{ text_ }}
            </bm-dropdown-item-toggle>
        </template>
    </bm-toolbar-element>
</template>
