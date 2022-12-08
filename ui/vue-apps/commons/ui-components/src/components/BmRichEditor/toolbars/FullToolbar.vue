<template>
    <bm-button-toolbar class="bm-rich-editor-toolbar-base full-toolbar" :class="{ disabled: disabled }" key-nav>
        <bm-button-group>
            <undo-button :editor="editor" :disabled="disabled || !formatState.canUndo" />
            <redo-button :editor="editor" :disabled="disabled || !formatState.canRedo" />
        </bm-button-group>

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
            <align-button :editor="editor" :disabled="disabled" />
            <bullet-list-button :editor="editor" :disabled="disabled" :is-bullet="!!formatState.isBullet" />
            <number-list-button :editor="editor" :disabled="disabled" :is-numbering="!!formatState.isNumbering" />
        </bm-button-group>
        <bm-button-group>
            <image-button :editor="editor" :disabled="disabled" />
            <link-button
                :editor="editor"
                :disabled="disabled"
                :is-link="!!formatState.canUnlink"
                @open-link-modal="$emit('open-link-modal', $event)"
            />
        </bm-button-group>
        <bm-button-group>
            <strike-through-button
                :editor="editor"
                :disabled="disabled"
                :is-strike-through="!!formatState.isStrikeThrough"
            />
            <block-quote-button :editor="editor" :disabled="disabled" :is-block-quote="!!formatState.isBlockQuote" />
            <table-button :editor="editor" :disabled="disabled" />
        </bm-button-group>
    </bm-button-toolbar>
</template>

<script>
import BmButtonGroup from "../../buttons/BmButtonGroup";
import BmButtonToolbar from "../../buttons/BmButtonToolbar";
import AlignButton from "../editorButtons/AlignButton";
import BackgroundColorButton from "../editorButtons/BackgroundColorButton";
import BlockQuoteButton from "../editorButtons/BlockQuoteButton";
import BoldButton from "../editorButtons/BoldButton";
import BulletListButton from "../editorButtons/BulletListButton";
import FontFamilyButton from "../editorButtons/FontFamilyButton";
import FontSizeButton from "../editorButtons/FontSizeButton";
import ImageButton from "../editorButtons/ImageButton";
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
        BmButtonGroup,
        BmButtonToolbar,
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
        disabled: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables.scss";
.full-toolbar {
    border-color: $neutral-fg;
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
