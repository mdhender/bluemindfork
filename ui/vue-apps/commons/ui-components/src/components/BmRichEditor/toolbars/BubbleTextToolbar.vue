<template>
    <bm-button-toolbar class="bm-rich-editor-toolbar-base bubble-text-toolbar" :class="{ disabled }">
        <bm-button-group>
            <font-family-button :editor="editor" :disabled="disabled" :selection-font-family="formatState.fontName" />
            <font-size-button :editor="editor" :disabled="disabled" :selection-font-size="formatState.fontSize" />
            <bold-button :editor="editor" :disabled="disabled" :is-bold="!!formatState.isBold" />
            <italic-button :editor="editor" :disabled="disabled" :is-italic="!!formatState.isItalic" />
            <underline-button :editor="editor" :disabled="disabled" :is-underline="!!formatState.isUnderline" />
        </bm-button-group>

        <bm-button-group class="color-group">
            <text-color-button :editor="editor" :disabled="disabled" />
            <background-color-button :editor="editor" :disabled="disabled" />
        </bm-button-group>

        <bm-button-group>
            <link-button
                :editor="editor"
                :disabled="disabled"
                :is-link="!!formatState.canUnlink"
                @open-link-modal="$emit('open-link-modal', $event)"
            />
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import BmButtonGroup from "../../buttons/BmButtonGroup";
import BmButtonToolbar from "../../buttons/BmButtonToolbar";
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
        BmButtonGroup,
        BmButtonToolbar,
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
        disabled: {
            type: Boolean,
            default: false
        }
    }
};
</script>
