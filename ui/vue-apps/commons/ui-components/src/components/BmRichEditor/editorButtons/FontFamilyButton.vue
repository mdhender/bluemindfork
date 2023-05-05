<template>
    <bm-form-font-selector
        :default-font="defaultFont"
        :selected="selectionFontFamily"
        :disabled="disabled"
        variant="inline"
        @input="setFontFamily"
    />
</template>
<script>
import { setFontName } from "roosterjs-editor-api";
import BmFormFontSelector from "../../form/BmFormFontSelector";

export default {
    name: "FontFamilyButton",
    components: { BmFormFontSelector },
    props: {
        selectionFontFamily: {
            type: String,
            default: ""
        },
        editor: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        defaultFont: {
            type: String,
            required: true
        }
    },
    methods: {
        setFontFamily(family) {
            setFontName(this.editor, family.value);
        }
    }
};
</script>
