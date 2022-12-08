<template>
    <bm-form-select
        :value="fontSize"
        class="font-size-button flex-fill"
        :options="sizes"
        variant="inline"
        :disabled="disabled"
        @input="setFontSize"
    />
</template>

<script>
import { setFontSize } from "roosterjs-editor-api";
import BmFormSelect from "../../form/BmFormSelect.vue";

const DEFAULT = "9pt";

export default {
    name: "FontSizeButton",
    components: { BmFormSelect },
    props: {
        selectionFontSize: {
            type: String,
            default: "10pt"
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
            fontSize: DEFAULT,
            sizes: [
                { value: "8pt", text: "8" },
                { value: "9pt", text: "9" },
                { value: "10pt", text: "10" },
                { value: "12pt", text: "12" },
                { value: "14pt", text: "14" },
                { value: "18pt", text: "18" },
                { value: "24pt", text: "24" }
            ]
        };
    },
    computed: {
        sizeValues() {
            return this.sizes.map(size => size.value);
        }
    },
    watch: {
        selectionFontSize: {
            handler(size) {
                this.fontSize = this.sizeValues.includes(size) ? size : DEFAULT;
            },
            immediate: true
        }
    },

    methods: {
        setFontSize(size) {
            if (size) {
                this.fontSize = size;
                setFontSize(this.editor, size);
            }
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables";

.font-size-button {
    .dropdown-item-content {
        margin-left: $sp-2;
    }
    .selected-text {
        float: right;
        min-width: 2em;
        margin-right: 0;
    }
}
</style>
