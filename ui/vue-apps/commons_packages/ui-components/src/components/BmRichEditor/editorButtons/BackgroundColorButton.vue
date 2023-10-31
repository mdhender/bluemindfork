<template>
    <bm-icon-dropdown
        ref="dropdown"
        class="background-color-button"
        variant="compact"
        size="lg"
        :disabled="disabled"
        split
        :title="$t('styleguide.rich_editor.text_highlight.tooltip')"
        @click="setBackgroundColor"
    >
        <bm-form-color-picker v-model="pickerColor" :colors="backgroundColors" @input="setBackgroundColor" />
        <template #icon>
            <bm-font-highlight-icon :color="disabled ? undefined : selectedBackgroundColor" />
        </template>
    </bm-icon-dropdown>
</template>

<script>
import { setBackgroundColor } from "roosterjs-editor-api";
import { getDarkColor } from "roosterjs-color-utils";
import darkifyingBaseLvalue from "../../../js/theming/darkifyingBaseLvalue";

import BmIconDropdown from "../../dropdown/BmIconDropdown";
import BmFontHighlightIcon from "../../BmFontHighlightIcon";
import BmFormColorPicker from "../../form/BmFormColorPicker";
import colors from "../../../css/exports/picker.scss";

const defaultColors = Object.values(colors);

export default {
    name: "BackgroundColorButton",
    components: { BmIconDropdown, BmFontHighlightIcon, BmFormColorPicker },
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
            selectedBackgroundColor: colors["yellow"],
            pickerColor: null
        };
    },
    mounted() {
        this.pickerColor = this.selectedBackgroundColor;
    },
    methods: {
        setBackgroundColor() {
            this.selectedBackgroundColor = this.pickerColor;
            const actualBackgroundColor =
                this.selectedBackgroundColor === "transparent" ? "#ffffff" : this.selectedBackgroundColor;
            setBackgroundColor(this.editor, {
                lightModeColor: actualBackgroundColor,
                darkModeColor: getDarkColor(actualBackgroundColor, darkifyingBaseLvalue())
            });
        }
    }
};
</script>
