<template>
    <div class="bm-form-color-picker">
        <bm-button-toolbar key-nav :aria-label="$t('styleguide.color-picker.toolbar.aria')">
            <bm-button v-for="color in allColors" :key="color" variant="text" @click="select(color)">
                <bm-color-badge :value="color" :selected="value === color" />
            </bm-button>
        </bm-button-toolbar>
        <bm-button variant="text" class="open-customizer-btn" @click="openCustomizer">
            {{ $t("styleguide.color-picker.customize") }}
        </bm-button>
        <b-form-input
            ref="customize-color-input"
            v-model="customizedColor"
            type="color"
            class="invisible customize-color-input"
        />
    </div>
</template>

<script>
import { BFormInput } from "bootstrap-vue";
import colors from "../../css/exports/picker.scss";
import BmButton from "../buttons/BmButton";
import BmButtonToolbar from "../buttons/BmButtonToolbar";
import BmColorBadge from "../BmColorBadge";

const defaultColors = Object.values(colors);

export default {
    name: "BmFormColorPicker",
    components: { BFormInput, BmButton, BmButtonToolbar, BmColorBadge },
    props: {
        value: {
            type: String,
            default: null
        },
        colors: {
            type: Array,
            default: () => defaultColors
        },
        pickDefault: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            customizedColor: null
        };
    },
    computed: {
        allColors() {
            let allColors = this.colors;
            if (this.customizedColor && !allColors.includes(this.customizedColor)) {
                allColors = allColors.concat([this.customizedColor]);
            }
            return allColors;
        }
    },
    watch: {
        customizedColor(newValue) {
            if (newValue !== this.value) {
                this.select(newValue);
            }
        }
    },
    created() {
        this.customizedColor = this.colors.includes(this.value) ? null : this.value;

        if (!this.value && this.pickDefault) {
            this.$emit("input", this.colors[Math.floor(Math.random() * this.colors.length)]);
        }
    },
    methods: {
        openCustomizer() {
            this.$refs["customize-color-input"].$el.click();
        },
        select(color) {
            this.$emit("input", color);
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_variables";

.bm-form-color-picker {
    display: inline-flex;
    flex-wrap: wrap;
    align-items: center;

    padding: $sp-2;

    .btn-toolbar {
        .bm-button {
            padding: $sp-3;
        }
    }

    .btn-text.open-customizer-btn {
        flex-grow: 1;
        padding-left: $sp-4;
        padding-right: $sp-4;
    }

    .customize-color-input {
        height: 0 !important;
        border: 0 !important;
        padding: 0 !important;
    }
}
</style>
