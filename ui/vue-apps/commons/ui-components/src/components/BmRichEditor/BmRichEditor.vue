<template>
    <div
        ref="rich-editor"
        class="bm-rich-editor d-flex flex-column"
        :class="{ 'has-border': hasBorder, disabled, 'has-focus': hasFocus }"
        @input="onChange"
        @contentchanged="onChange"
        @focusin="hasFocus = true"
        @focusout="hasFocus = false"
        @dragstart="internalDrag = true"
        @dragend="internalDrag = false"
    >
        <template v-if="editor && !disabled && bubbleToolbar.focusedPosition">
            <bubble-toolbar
                v-show="bubbleToolbar.show"
                :selection="selection"
                :editor="editor"
                :focused-position="bubbleToolbar.focusedPosition"
                :format-state="formatState"
                @open-link-modal="openLinkModal"
                @click.native="updateFormatState"
            />
        </template>
        <div class="main-area flex-fill overflow-auto">
            <div ref="roosterjs-container" class="roosterjs-container" @drop="onDrop" />
            <table-toolbar v-if="editor && tableToolbar.show" :editor="editor" :table="tableToolbar.table" />
            <global-events @dragover.capture="onDragover" />
            <slot />
        </div>
        <transition name="slide-fade">
            <full-toolbar
                v-if="showToolbar && editor"
                class="justify-content-end"
                :editor="editor"
                :format-state="formatState"
                :disabled="disabled"
                full-toolbar
                @open-link-modal="openLinkModal"
                @click.native="updateFormatState"
            />
        </transition>
        <set-link-modal v-if="linkModal.show" :editor="editor" :init-link="linkModal" @close="onLinkModalClose" />
    </div>
</template>

<script>
import throttle from "lodash.throttle";
import debounce from "lodash.debounce";
import { createLink, getFormatState, insertImage } from "roosterjs-editor-api";
import { Editor } from "roosterjs-editor-core";
import { getPositionRect, Position } from "roosterjs-editor-dom";
import { ContentEdit, CutPasteListChain, HyperLink, ImageEdit, Paste, TableResize } from "roosterjs-editor-plugins";
import { PositionType, QueryScope } from "roosterjs-editor-types";
import GlobalEvents from "vue-global-events";

import BmEditorEventListener from "./plugins/BmEditorEventListener";
import BubbleToolbar from "./toolbars/BubbleToolbar";
import TableToolbar from "./toolbars/TableToolbar";
import SetLinkModal from "./modals/SetLinkModal";
import FullToolbar from "./toolbars/FullToolbar";
import InsertContentMixin from "./mixins/InsertContentMixin";
import Default from "./bmPlugins/Default";
import NonEditable, { NON_EDITABLE_CONTENT_DROP_ID } from "./bmPlugins/NonEditable";

import BmRichEditorRegistry from "./BmRichEditorRegistry";

export default {
    name: "BmRichEditor",
    components: { BubbleToolbar, GlobalEvents, SetLinkModal, FullToolbar, TableToolbar },
    mixins: [InsertContentMixin],
    props: {
        name: {
            type: String,
            required: true
        },
        initValue: {
            type: String,
            required: true
        },
        hasBorder: {
            type: Boolean,
            default: false
        },
        showToolbar: {
            type: Boolean,
            default: true
        },
        disabled: {
            type: Boolean,
            default: false
        },
        adaptOutput: {
            type: Function,
            default: () => {}
        }
    },
    data() {
        return {
            container: null,
            editor: null,
            formatState: {},
            hasFocus: false,
            selection: null,
            bubbleToolbar: { show: false, focusedPosition: null },
            tableToolbar: { show: false, table: null },
            internalDrag: false,
            linkModal: { show: false, url: "", text: "" },
            plugins: [new Default(this), new NonEditable(this)],
            isReady: false
        };
    },
    watch: {
        disabled(newVal) {
            this.disableContainer(newVal);
        },
        name(newVal, oldVal) {
            BmRichEditorRegistry.unregister(oldVal);
            BmRichEditorRegistry.register(newVal, this);
        }
    },
    created() {
        BmRichEditorRegistry.register(this.name, this);
    },
    mounted() {
        this.container = this.$refs["roosterjs-container"];
        document.addEventListener("selectionchange", this.onSelection);
        window.addEventListener("scroll", this.onScroll, true);

        this.initializeEditor();
        this.disableContainer(this.disabled);
        this.isReady = true;
    },
    beforeDestroy() {
        document.removeEventListener("selectionchange", this.onSelection);
        window.removeEventListener("scroll", this.onScroll, true);
        this.editor.dispose();
        BmRichEditorRegistry.unregister(this.name);
    },
    methods: {
        // can be used by parent
        setContent(newContent) {
            this.editor.setContent(newContent);
        },
        getContent() {
            return this.editor.getContent();
        },

        // internal
        onDrop(event) {
            if (!this.internalDrag) {
                // if internalDrag is false it means file comes from filesystem
                event.preventDefault(); // prevent opening the file into a new tab
                const file = event.dataTransfer.files[0];

                if (file && file.type.match(new RegExp("image/(jpeg|jpg|png|gif)", "i"))) {
                    const node = event.target.nextSibling || event.target;
                    const range = document.createRange();
                    range.setStart(node, 0);
                    range.setEnd(node, 0);
                    this.editor.select(range);
                    insertImage(this.editor, file);
                }
            } else if (
                event.dataTransfer.getData(NON_EDITABLE_CONTENT_DROP_ID) &&
                !this.nonEditableContent.contains(event.target)
            ) {
                if (event.target.nodeName === "DIV") {
                    event.target.appendChild(this.nonEditableContent);
                } else {
                    event.target.after(this.nonEditableContent);
                }
            }
        },
        onDragover(event) {
            if (this.internalDrag) {
                event.stopPropagation(); // dont inform parent if it's an internal drag
            }
            if (event.dataTransfer.types.length === 1 && event.dataTransfer.types[0] === NON_EDITABLE_CONTENT_DROP_ID) {
                event.preventDefault(); // necessary for the drop event to be triggered
            }
        },
        updateFormatState() {
            if (this.editor) {
                this.formatState = getFormatState(this.editor);
            }
        },

        onChange: throttle(function () {
            this.adaptOutput(this.container);
            this.$emit("input", this.editor.getContent());
        }, 200),

        onSelection: debounce(function () {
            const selection = document.getSelection();
            const isInEditor =
                this.container.contains(selection.anchorNode) && this.container.contains(selection.focusNode);
            if (!isInEditor) {
                this.bubbleToolbar.show = false;
                this.tableToolbar.show = false;
                return;
            }
            this.selection = selection;
            this.updateFormatState();

            if (this.isCaretOnLink() || !this.selection.isCollapsed) {
                this.bubbleToolbar.focusedPosition = this.getFocusedPosition();
                this.bubbleToolbar.show = !!this.bubbleToolbar.focusedPosition;
            } else {
                this.bubbleToolbar.show = false;
            }

            const parentTable = getTableParentNode(this.selection.focusNode, this.container);
            if (parentTable) {
                this.tableToolbar.table = parentTable;
                this.tableToolbar.show = true;
            } else {
                this.tableToolbar.show = false;
            }
        }, 200),
        getFocusedPosition() {
            let focus = this.editor.getFocusedPosition();
            if (
                !this.selection.isCollapsed &&
                !focus.isAtEnd &&
                focus.offset === 0 &&
                focus.node.previousElementSibling
            ) {
                focus = new Position(focus.node.previousElementSibling, PositionType.End);
            }
            return getPositionRect(focus);
        },
        onScroll() {
            this.bubbleToolbar.show = false;
            this.tableToolbar.show = false;
            this.onSelection();
        },
        initializeEditor() {
            const bmPlugins = [new BmEditorEventListener(this.$refs["rich-editor"])];
            const roosterPlugins = [
                new ImageEdit({ preserveRatio: true, borderColor: "var(--secondary-fg)" }),
                new ContentEdit(),
                new Paste(),
                new CutPasteListChain(),
                new HyperLink(),
                new TableResize()
            ];
            const options = {
                initialContent: this.initValue,
                doNotAdjustEditorColor: true,
                plugins: [...bmPlugins, ...roosterPlugins]
            };
            this.editor = new Editor(this.container, options);
        },
        openLinkModal() {
            this.bubbleToolbar.show = false;
            const existingLink = this.editor.queryElements("a[href]", QueryScope.OnSelection)[0];
            this.linkModal.url = existingLink?.href || "";
            this.linkModal.text = existingLink?.textContent || this.editor.getSelectionRange()?.toString() || "";
            this.linkModal.show = true;
        },
        onLinkModalClose({ url, text, mustCreate }) {
            this.linkModal.show = false;
            this.bubbleToolbar.show = false;
            if (mustCreate) {
                createLink(this.editor, url, url, text);
            }
        },
        isCaretOnLink() {
            return this.formatState.canUnlink && this.selection.anchorNode.parentElement.nodeName === "A";
        },
        disableContainer(value) {
            this.container.setAttribute("contenteditable", !value);
        }
    },
    constants: { NEW_LINE: `<div><br/></div>`, NON_EDITABLE_CONTENT_DROP_ID }
};

function getTableParentNode(node, containerNode) {
    while (node && node !== containerNode) {
        node = node.parentNode;
        if (node && node.nodeName && node.nodeName === "TABLE") {
            return node;
        }
    }
    return undefined;
}
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.bm-rich-editor {
    .roosterjs-container {
        outline: none;
    }

    $padding: $sp-5;
    &.has-border .main-area {
        padding: $padding;
    }

    .bm-rich-editor-toolbar-base {
        padding: $input-border-width;
    }

    &.has-border {
        border: $input-border-width solid $neutral-fg;
    }
    &.has-border:not(.disabled) {
        &.hover,
        &:hover {
            border-color: $neutral-fg-hi1;
            .full-toolbar {
                border-top-color: $neutral-fg-hi1;
            }
        }
        &.has-focus {
            border: 2 * $input-border-width solid $secondary-fg;
            .main-area {
                padding: calc(#{$padding} - #{$input-border-width});
                padding-bottom: $padding;
            }
            .full-toolbar {
                border-top: 2 * $input-border-width solid $secondary-fg;
                padding: 0;
            }
        }
    }

    &.disabled {
        background-color: $input-disabled-bg;
        border-color: $neutral-fg-disabled;
        .roosterjs-container > * {
            color: $neutral-fg-lo1;
        }
    }

    .roosterjs-container {
        min-height: 10rem;
        img {
            vertical-align: unset; // reset bootstrap property set in reboot.scss
        }
    }

    .slide-fade-enter-active,
    .slide-fade-leave-active {
        transition: all 0.1s ease-out;
    }
    .slide-fade-enter,
    .slide-fade-leave-to {
        transform: translateY(20px);
    }
}

// Toolbar common styles
.bm-rich-editor-toolbar-base {
    & > .btn-group:not(:last-child) {
        border-right: 1px $neutral-fg-lo3 solid;
    }
}
</style>
