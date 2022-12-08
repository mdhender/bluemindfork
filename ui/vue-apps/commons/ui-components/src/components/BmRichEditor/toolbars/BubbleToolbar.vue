<template>
    <div class="bm-rich-editor-toolbar-base bm-rich-editor-bubble-toolbar shadow-sm z-index-110" :style="style">
        <bubble-link-toolbar
            v-if="bubbleToolbarType === BUBBLE_TOOLBAR_TYPES.LINK"
            ref="bubble-link-toolbar"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
        <bubble-text-toolbar
            v-else-if="bubbleToolbarType === BUBBLE_TOOLBAR_TYPES.TEXT"
            v-bind="[$attrs, $props]"
            v-on="$listeners"
        />
    </div>
</template>

<script>
import BubbleTextToolbar from "./BubbleTextToolbar";
import BubbleLinkToolbar from "./BubbleLinkToolbar";

const BUBBLE_TOOLBAR_TYPES = {
    LINK: "link",
    TEXT: "text"
};

export default {
    name: "BubbleToolbar",
    components: { BubbleTextToolbar, BubbleLinkToolbar },
    props: {
        editor: {
            type: Object,
            required: true
        },
        focusedPosition: {
            type: Object,
            required: true
        },
        // eslint-disable-next-line vue/require-prop-types
        selection: {
            // Should be type [Selection, Boolean] but the "Selection" type is unknown,
            required: true
        },
        formatState: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            BUBBLE_TOOLBAR_TYPES,
            toolbarWidth: null
        };
    },
    computed: {
        left() {
            return this.focusedPosition.left;
        },
        bottom() {
            return this.focusedPosition.bottom;
        },
        top() {
            return this.focusedPosition.top - 35; // to avoid hiding focused position
        },
        style() {
            if (this.left > document.body.clientWidth - this.toolbarWidth) {
                return `top: ${this.bottom}px; left: ${this.left - this.toolbarWidth}px;`;
            }
            return `top: ${this.top}px; left: ${this.left}px;`;
        },
        bubbleToolbarType() {
            if (
                this.selection &&
                this.selection.isCollapsed &&
                this.formatState.canUnlink &&
                this.selection.anchorNode.parentElement.nodeName === "A"
            ) {
                return BUBBLE_TOOLBAR_TYPES.LINK;
            } else if (
                this.selection &&
                this.formatState.canUnlink &&
                this.selection.anchorNode.parentNode.isEqualNode(this.selection.focusNode.parentNode) &&
                this.selection.anchorNode.parentNode.nodeName === "A"
            ) {
                return BUBBLE_TOOLBAR_TYPES.LINK;
            } else {
                return BUBBLE_TOOLBAR_TYPES.TEXT;
            }
        }
    },
    updated() {
        if (this.bubbleToolbarType === BUBBLE_TOOLBAR_TYPES.LINK) {
            this.$refs["bubble-link-toolbar"].setUrl();
        }
    },
    mounted() {
        this.toolbarWidth = this.$el.offsetWidth;
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables.scss";

.bm-rich-editor-bubble-toolbar {
    position: fixed;
    margin-bottom: $sp-3;
    background-color: $surface;
    .btn-toolbar {
        flex-wrap: nowrap;
    }
    .btn-group {
        flex-wrap: nowrap;
    }
}
</style>
