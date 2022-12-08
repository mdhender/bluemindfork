<template>
    <bm-form-select
        :value="fontFamily"
        class="font-family-button flex-fill"
        :options="families"
        variant="inline"
        :disabled="disabled"
        @input="setFontfamily"
    >
        <template v-slot:selected="slotProps">
            <span v-if="slotProps.selected" :class="`${slotProps.selected.class} selected-text`">
                {{ slotProps.selected.text }}</span
            >
        </template>
        <template v-slot:item="slotProps">
            <span :class="slotProps.item.class"> {{ slotProps.item.text }}</span>
        </template>
    </bm-form-select>
</template>
<script>
import { setFontName } from "roosterjs-editor-api";
import BmFormSelect from "../../form/BmFormSelect";

const DEFAULT = "Montserrat, montserrat, Source Sans, Helvetica Neue, Helvetica, Arial, sans-serif";

export default {
    name: "FontFamilyButton",
    components: { BmFormSelect },
    props: {
        selectionFontFamily: {
            type: String,
            default: DEFAULT
        },
        editor: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            fontFamily: DEFAULT,
            families: [
                {
                    value: "Red Hat Mono, Courier New, Courier, Lucida Sans Typewriter, Lucida Typewriter, monospace",
                    text: "Mono",
                    class: "mono"
                },
                {
                    value: DEFAULT,
                    text: this.$t("common.default"),
                    class: "montserrat"
                },
                {
                    value:
                        "Garamond, Apple Garamond, Palatino Linotype, Palatino, Baskerville, Baskerville Old Face, serif",
                    text: "Garamond",
                    class: "garamond"
                },
                {
                    value:
                        "Georgia, Constantia, Lucida Bright, Lucidabright, Lucida Serif, Lucida, DejaVu Serif, serif",
                    text: "Georgia",
                    class: "georgia"
                },
                {
                    value: "Helvetica Neue, Helvetica, Nimbus Sans, Arial, sans-serif",
                    text: "Helvetica",
                    class: "helvetica"
                },
                {
                    value:
                        "Verdana, Verdana Ref, Corbel, Lucida Grande, Lucida Sans Unicode, Lucida Sans, DejaVu Sans, Liberation Sans, sans-serif",
                    text: "Verdana",
                    class: "verdana"
                }
            ]
        };
    },
    watch: {
        selectionFontFamily: {
            handler(selectionFontFamily) {
                const familiesListValues = this.families.map(family => ({
                    ...family,
                    value: family.value.split(", ")
                }));
                const maxLength = getLongestValue(familiesListValues).value.length;

                const selectionFontsListValues = selectionFontFamily.replaceAll('"', "").split(",");
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
                    this.fontFamily = DEFAULT;
                }
            },
            immediate: true
        }
    },
    methods: {
        setFontfamily(family) {
            if (family) {
                this.fontFamily = family;
                setFontName(this.editor, family);
            }
        }
    }
};

function getLongestValue(obj) {
    return obj.reduce((a, b) => (a?.value?.length > b.value.length ? a : b));
}
</script>

<style lang="scss">
@import "../../../css/_variables";

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
