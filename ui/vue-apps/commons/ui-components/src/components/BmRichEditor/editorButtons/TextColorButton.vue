<template>
    <bm-icon-dropdown
        ref="text-color-button"
        class="text-color-button"
        variant="compact"
        size="lg"
        :disabled="disabled"
        split
        :title="$t('styleguide.rich_editor.text_color.tooltip')"
        @click="setTextColor"
    >
        <bm-form-color-picker v-model="pickerColor" :colors="textColors" @input="setTextColor" />
        <template #icon>
            <bm-font-color-icon :color="disabled ? undefined : selectedTextColor" />
        </template>
    </bm-icon-dropdown>
</template>

<script>
import { setTextColor } from "roosterjs-editor-api";
import { getDarkColor } from "roosterjs-color-utils";
import darkifyingBaseLvalue from "../../../js/theming/darkifyingBaseLvalue";

import BmIconDropdown from "../../dropdown/BmIconDropdown";
import BmFontColorIcon from "../../BmFontColorIcon";
import BmFormColorPicker from "../../form/BmFormColorPicker";
import colors from "../../../css/exports/picker.scss";

const defaultColors = Object.values(colors);

export default {
    name: "TextColorButton",
    components: { BmIconDropdown, BmFontColorIcon, BmFormColorPicker },
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
    mounted() {
        this.pickerColor = this.selectedTextColor;
    },
    methods: {
        setTextColor() {
            this.selectedTextColor = this.pickerColor;
            setTextColor(this.editor, {
                lightModeColor: this.selectedTextColor,
                darkModeColor: getDarkColor(this.selectedTextColor, darkifyingBaseLvalue())
            });
        }
    }
};
</script>
