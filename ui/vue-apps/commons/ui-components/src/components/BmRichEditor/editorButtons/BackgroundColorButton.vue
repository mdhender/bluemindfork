<template>
    <bm-icon-dropdown
        ref="dropdown"
        class="background-color-button"
        variant="compact"
        size="lg"
        icon="text-highlight"
        :style="iconStyle"
        :disabled="disabled"
        split
        :title="$t('styleguide.rich_editor.text_highlight.tooltip')"
        @click="setBackgroundColor"
    >
        <bm-form-color-picker v-model="pickerColor" :colors="backgroundColors" @input="setBackgroundColor" />
    </bm-icon-dropdown>
</template>

<script>
import { setBackgroundColor } from "roosterjs-editor-api";
import BmIconDropdown from "../../dropdown/BmIconDropdown";
import BmFormColorPicker from "../../form/BmFormColorPicker";
import colors from "../../../css/exports/picker.scss";

const defaultColors = Object.values(colors);

export default {
    name: "BackgroundColorButton",
    components: { BmIconDropdown, BmFormColorPicker },
    props: {
        disabled: {
            type: Boolean,
            required: true
        },
        editor: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            backgroundColors: ["transparent", ...defaultColors],
            selectedBackgroundColor: colors["yellow-light"],
            pickerColor: null
        };
    },
    computed: {
        iconStyle() {
            if (this.disabled) {
                return {};
            }
            return { color: this.selectedBackgroundColor, stroke: "black", "stroke-width": "0.1px" };
        }
    },
    mounted() {
        this.pickerColor = this.selectedBackgroundColor;
    },
    methods: {
        setBackgroundColor() {
            this.selectedBackgroundColor = this.pickerColor;
            setBackgroundColor(this.editor, this.selectedBackgroundColor);
        }
    }
};
</script>
<style lang="scss">
@import "../../../css/_variables.scss";
.background-color-button {
    .bm-form-color-picker .btn-toolbar {
        stroke: initial;
    }
    .btn:not(.dropdown-toggle-split) {
        color: inherit !important;
        &.disabled {
            color: $neutral-fg-disabled !important;
        }
    }
}
</style>
