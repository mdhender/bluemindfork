<template>
    <full-toolbar
        v-if="editor_ && isReady"
        :editor="editor_.editor"
        :format-state="editor_.formatState"
        :disabled="disabled || editor_.disabled"
        :class="classes"
        @open-link-modal="editor_.openLinkModal"
        @click.native="editor_.updateFormatState"
    />
</template>
<script>
import Vue from "vue";
import FullToolbar from "./toolbars/FullToolbar.vue";
import BmRichEditorRegistry from "./BmRichEditorRegistry";

export default {
    name: "BmRichEditorToolbar",
    components: { FullToolbar },
    props: {
        editor: {
            type: [String, Vue],
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        align: {
            type: String,
            default: "left"
        }
    },
    computed: {
        editor_() {
            if (typeof this.editor === "string") {
                return BmRichEditorRegistry.get(this.editor);
            } else {
                return this.editor;
            }
        },
        isReady() {
            return this.editor_?.isReady;
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
