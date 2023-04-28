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
            <span v-if="slotProps.selected" :class="`${slotProps.selected.id} selected-text`">
                {{ slotProps.selected.text }}</span
            >
        </template>
        <template v-slot:item="slotProps">
            <span :class="slotProps.item.id"> {{ slotProps.item.text }}</span>
        </template>
    </bm-form-select>
</template>

<script>
import BmFormSelect from "./BmFormSelect";
import DEFAULT from "../../js/defaultFont";

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
            default: "inline"
        }
    },
    data() {
        return {
            fontFamily: this.selected,
            families: [
                {
                    value: "Red Hat Mono, Courier New, Courier, Lucida Sans Typewriter, Lucida Typewriter, monospace",
                    text: "Mono",
                    id: "mono"
                },
                {
                    value: DEFAULT,
                    text: this.$t("common.default"),
                    id: "montserrat"
                },
                {
                    value: "Garamond, Apple Garamond, Palatino Linotype, Palatino, Baskerville, Baskerville Old Face, serif",
                    text: "Garamond",
                    id: "garamond"
                },
                {
                    value: "Georgia, Constantia, Lucida Bright, Lucidabright, Lucida Serif, Lucida, DejaVu Serif, serif",
                    text: "Georgia",
                    id: "georgia"
                },
                {
                    value: "Helvetica Neue, Helvetica, Nimbus Sans, Arial, sans-serif",
                    text: "Helvetica",
                    id: "helvetica"
                },
                {
                    value: "Verdana, Verdana Ref, Corbel, Lucida Grande, Lucida Sans Unicode, Lucida Sans, DejaVu Sans, Liberation Sans, sans-serif",
                    text: "Verdana",
                    id: "verdana"
                }
            ]
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
                    this.fontFamily = this.families.find(family => family.id === this.defaultFont).value;
                }
            },
            immediate: true
        }
    },
    created() {
        if (!this.selected) {
            this.fontFamily = this.families.find(family => family.id === this.defaultFont).value;
        }
    },
    methods: {
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
    return obj.reduce((a, b) => (a?.value?.length > b.value.length ? a : b));
}
</script>

<style lang="scss">
@import "../../css/_variables";

.font-family-button {
    & > .btn {
        font-weight: normal;
    }
    .mono {
        font-family: "Red Hat Mono", "Courier New", Courier, "Lucida Sans Typewriter", "Lucida Typewriter", monospace;
    }
    .georgia {
        font-family: "Georgia", "Constantia", "Lucida Bright", "Lucidabright", "Lucida Serif", Lucida, "DejaVu Serif",
            serif;
    }
    .garamond {
        font-family: Garamond, "Apple Garamond", "Palatino Linotype", "Palatino", "Baskerville", "Baskerville Old Face",
            serif;
    }
    .helvetica {
        font-family: "Helvetica Neue", Helvetica, "Nimbus Sans", "Arial", sans-serif;
    }
    .monserrat {
        font-family: "Montserrat", "montserrat", "Source Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
    }
    .verdana {
        font-family: Verdana, "Verdana Ref", "Corbel", "Lucida Grande", "Lucida Sans Unicode", "Lucida Sans",
            "DejaVu Sans", "Liberation Sans", sans-serif;
    }
}
</style>
