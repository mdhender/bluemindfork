<template>
    <bm-toolbar class="bm-rich-editor-toolbar-base bubble-text-toolbar" :class="{ disabled }">
        <font-family-button
            :editor="editor"
            :disabled="disabled"
            :selection-font-family="formatState.fontName"
            :default-font="defaultFont"
            :extra-font-families="extraFontFamilies"
        />
        <font-size-button :editor="editor" :disabled="disabled" :selection-font-size="formatState.fontSize" />
        <bold-button :editor="editor" :disabled="disabled" :is-bold="!!formatState.isBold" />
        <italic-button :editor="editor" :disabled="disabled" :is-italic="!!formatState.isItalic" />
        <underline-button :editor="editor" :disabled="disabled" :is-underline="!!formatState.isUnderline" />
        <bm-toolbar-divider />

        <text-color-button :editor="editor" :disabled="disabled" />
        <background-color-button :editor="editor" :disabled="disabled" />
        <bm-toolbar-divider />
        <link-button
            :editor="editor"
            :disabled="disabled"
            :is-link="!!formatState.canUnlink"
            @open-link-modal="$emit('open-link-modal', $event)"
        />
    </bm-toolbar>
</template>

<script>
import BmToolbar from "../../BmToolbar/BmToolbar";
import BmToolbarDivider from "../../BmToolbar/BmToolbarDivider";
import BackgroundColorButton from "../editorButtons/BackgroundColorButton";
import BoldButton from "../editorButtons/BoldButton";
import FontFamilyButton from "../editorButtons/FontFamilyButton";
import FontSizeButton from "../editorButtons/FontSizeButton";
import ItalicButton from "../editorButtons/ItalicButton";
import LinkButton from "../editorButtons/LinkButton";
import TextColorButton from "../editorButtons/TextColorButton";
import UnderlineButton from "../editorButtons/UnderlineButton";

export default {
    name: "BubbleTextToolbar",
    components: {
        BmToolbar,
        BmToolbarDivider,
        BackgroundColorButton,
        BoldButton,
        FontSizeButton,
        FontFamilyButton,
        ItalicButton,
        LinkButton,
        TextColorButton,
        UnderlineButton
    },
    props: {
        editor: {
            type: Object,
            required: true
        },
        formatState: {
            type: Object,
            required: true
        },
        defaultFont: {
            type: String,
            required: true
        },
        extraFontFamilies: {
            type: Array,
            default: () => []
        },
        disabled: {
            type: Boolean,
            default: false
        }
    }
};
</script>
