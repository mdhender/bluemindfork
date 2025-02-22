<template>
    <bm-toolbar
        class="bm-rich-editor-toolbar-base full-toolbar"
        :class="{ disabled: disabled }"
        key-nav
        align-right
        menu-icon="3dots-vertical"
        menu-icon-size="lg"
        menu-icon-variant="compact"
    >
        <undo-button :editor="editor" :disabled="disabled || !formatState.canUndo" />
        <redo-button :editor="editor" :disabled="disabled || !formatState.canRedo" />
        <bm-toolbar-divider />

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

        <align-button :editor="editor" :disabled="disabled" />
        <bm-toolbar-button-group>
            <indent-less-button :editor="editor" :disabled="disabled" />
            <indent-more-button :editor="editor" :disabled="disabled" />
        </bm-toolbar-button-group>
        <bullet-list-button :editor="editor" :disabled="disabled" :is-bullet="!!formatState.isBullet" />
        <number-list-button :editor="editor" :disabled="disabled" :is-numbering="!!formatState.isNumbering" />
        <bm-toolbar-divider />
        <image-button :editor="editor" :disabled="disabled" />
        <link-button
            :editor="editor"
            :disabled="disabled"
            :is-link="!!formatState.canUnlink"
            @open-link-modal="$emit('open-link-modal', $event)"
        />
        <bm-toolbar-divider />
        <strike-through-button
            :editor="editor"
            :disabled="disabled"
            :is-strike-through="!!formatState.isStrikeThrough"
        />
        <block-quote-button :editor="editor" :disabled="disabled" :is-block-quote="!!formatState.isBlockQuote" />
        <table-button :editor="editor" :disabled="disabled" />
    </bm-toolbar>
</template>

<script>
import BmToolbarButtonGroup from "../../BmToolbar/BmToolbarButtonGroup";
import BmToolbarDivider from "../../BmToolbar/BmToolbarDivider";

import BmToolbar from "../../BmToolbar/BmToolbar";
import AlignButton from "../editorButtons/AlignButton";
import BackgroundColorButton from "../editorButtons/BackgroundColorButton";
import BlockQuoteButton from "../editorButtons/BlockQuoteButton";
import BoldButton from "../editorButtons/BoldButton";
import BulletListButton from "../editorButtons/BulletListButton";
import FontFamilyButton from "../editorButtons/FontFamilyButton";
import FontSizeButton from "../editorButtons/FontSizeButton";
import ImageButton from "../editorButtons/ImageButton";
import IndentLessButton from "../editorButtons/IndentLessButton";
import IndentMoreButton from "../editorButtons/IndentMoreButton";
import ItalicButton from "../editorButtons/ItalicButton";
import LinkButton from "../editorButtons/LinkButton";
import NumberListButton from "../editorButtons/NumberListButton";
import RedoButton from "../editorButtons/RedoButton";
import StrikeThroughButton from "../editorButtons/StrikeThroughButton";
import TableButton from "../editorButtons/TableButton";
import TextColorButton from "../editorButtons/TextColorButton";
import UnderlineButton from "../editorButtons/UnderlineButton";
import UndoButton from "../editorButtons/UndoButton";

export default {
    name: "FullToolbar",
    components: {
        BmToolbarButtonGroup,
        BmToolbarDivider,
        BmToolbar,
        AlignButton,
        BackgroundColorButton,
        BlockQuoteButton,
        BoldButton,
        BulletListButton,
        FontSizeButton,
        FontFamilyButton,
        ImageButton,
        ItalicButton,
        LinkButton,
        NumberListButton,
        IndentLessButton,
        IndentMoreButton,
        RedoButton,
        StrikeThroughButton,
        TableButton,
        TextColorButton,
        UnderlineButton,
        UndoButton
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
        disabled: {
            type: Boolean,
            default: false
        },
        extraFontFamilies: {
            type: Array,
            default: () => []
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/utils/variables.scss";
.full-toolbar {
    border-color: $neutral-fg-lo1;
    border-style: solid;
    border-width: 1px 0 0 0;
    &.disabled {
        border-color: $neutral-fg-disabled;
    }

    .btn-icon-compact {
        outline-offset: -$input-border-width;
    }
}
</style>
