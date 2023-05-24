<template>
    <full-toolbar
        v-if="editor && isReady"
        :editor="editor.editor"
        :format-state="editor.formatState"
        :disabled="disabled || editor.disabled"
        :class="classes"
        :default-font="defaultFontFamily"
        :extra-font-families="extraFontFamilies"
        @open-link-modal="editor.openLinkModal"
        @click.native="editor.updateFormatState"
    />
</template>
<script>
import FullToolbar from "./toolbars/FullToolbar.vue";

export default {
    name: "BmRichEditorToolbar",
    components: { FullToolbar },
    props: {
        editor: {
            type: [Object],
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        align: {
            type: String,
            default: "left"
        },
        defaultFontFamily: {
            type: String,
            required: true
        },
        extraFontFamilies: {
            type: Array,
            default: () => []
        }
    },
    computed: {
        isReady() {
            return this.editor?.isReady;
        },
        classes() {
            return {
                "bm-rich-editor-toolbar": true,
                "justify-content-end": this.align === "right",
                "justify-content-center": this.align === "center"
            };
        }
    }
};
</script>
<style lang="scss">
@import "../../css/_variables.scss";
.bm-rich-editor-toolbar {
    border-width: 1px;
}
</style>
