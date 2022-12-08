<template>
    <bm-icon-dropdown
        ref="text-color-button"
        class="text-color-button"
        variant="compact"
        size="lg"
        icon="text-format"
        :style="iconStyle"
        :disabled="disabled"
        split
        :title="$t('styleguide.rich_editor.text_color.tooltip')"
        @click="setTextColor"
    >
        <bm-form-color-picker v-model="pickerColor" :colors="textColors" @input="setTextColor" />
    </bm-icon-dropdown>
</template>

<script>
import { setTextColor } from "roosterjs-editor-api";
import BmIconDropdown from "../../dropdown/BmIconDropdown";
import BmFormColorPicker from "../../form/BmFormColorPicker";
import colors from "../../../css/exports/picker.scss";

const defaultColors = Object.values(colors);

export default {
    name: "TextColorButton",
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
            textColors: defaultColors,
            selectedTextColor: colors.red,
            pickerColor: null
        };
    },
    computed: {
        iconStyle() {
            if (this.disabled) {
                return {};
            }
            return { color: this.selectedTextColor, stroke: "black", "stroke-width": "0.1px" };
        }
    },
    mounted() {
        this.pickerColor = this.selectedTextColor;
    },
    methods: {
        setTextColor() {
            this.selectedTextColor = this.pickerColor;
            setTextColor(this.editor, this.selectedTextColor);
        }
    }
};
</script>
<style lang="scss">
@import "../../../css/_variables.scss";
.text-color-button {
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
