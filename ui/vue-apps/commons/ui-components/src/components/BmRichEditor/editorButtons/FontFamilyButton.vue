<template>
    <bm-form-font-selector :selected="selected" :disabled="disabled" @input="setFontFamily" />
</template>
<script>
import { setFontName } from "roosterjs-editor-api";
import BmFormFontSelector from "../../form/BmFormFontSelector";
import DEFAULT from "../../../js/defaultFont";

export default {
    name: "FontFamilyButton",
    components: { BmFormFontSelector },
    props: {
        selected: {
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
    methods: {
        setFontFamily(family) {
            setFontName(this.editor, family);
        }
    }
};
</script>
