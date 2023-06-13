<template>
    <bm-form-select
        class="font-family-button flex-fill"
        :value="fontFamily"
        :options="families"
        :variant="variant"
        :disabled="disabled"
        @input="setFontFamily"
    >
        <template v-slot:selected="slotProps">
            <span
                v-if="slotProps.selected"
                :style="{ fontFamily: fontFamilyValue(slotProps.selected.id) }"
                class="selected-text"
            >
                {{ slotProps.selected.text }}</span
            >
        </template>
        <template v-slot:item="slotProps">
            <span :style="{ fontFamily: fontFamilyValue(slotProps.item.id) }"> {{ slotProps.item.text }}</span>
        </template>
    </bm-form-select>
</template>

<script>
import BmFormSelect from "./BmFormSelect";
import FONT_FAMILIES, { fontFamilyByID } from "../../js/fontFamilies";

export default {
    name: "BmFormFontSelector",
    components: { BmFormSelect },
    props: {
        selected: {
            type: String,
            default: ""
        },
        defaultFont: {
            type: String,
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "inline"].includes(value);
            }
        },
        extraFontFamilies: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
            fontFamily: this.selected,
            families: [...FONT_FAMILIES, ...this.extraFontFamilies]
        };
    },
    watch: {
        selected: {
            handler(selectedFontFamily) {
                const familiesListValues = this.families.map(family => ({
                    ...family,
                    value: family.value.split(", ")
                }));
                const maxLength = getLongestValue(familiesListValues).value.length;

                const selectionFontsListValues = selectedFontFamily.replaceAll('"', "").split(",");
                const found = selectionFontsListValues.find(element => {
                    const font = element.trim();

                    for (let i = 0; i < maxLength; i++) {
                        const closestFontMatch = familiesListValues.find(family => {
                            return family.value[i] && family.value[i].trim() === font;
                        });
                        if (closestFontMatch) {
                            this.fontFamily = closestFontMatch.value.join(", ");
                            return true;
                        }
                    }
                });
                if (!found) {
                    this.fontFamily = this.fontFamilyValue(this.defaultFont);
                }
            },
            immediate: true
        }
    },
    created() {
        if (!this.selected) {
            this.fontFamily = this.fontFamilyValue(this.defaultFont);
        }
    },
    methods: {
        fontFamilyValue(fontId) {
            return fontFamilyByID(fontId, this.extraFontFamilies);
        },
        setFontFamily(family) {
            this.fontFamily = family;
            this.$emit(
                "input",
                this.families.find(f => f.value === family)
            );
        }
    }
};

function getLongestValue(obj) {
    return obj.reduce((a, b) => (a.value.length > b.value.length ? a : b));
}
</script>

<style lang="scss">
@import "../../css/utils/variables";

.font-family-button {
    & > .btn {
        font-weight: normal;
    }
}
</style>
