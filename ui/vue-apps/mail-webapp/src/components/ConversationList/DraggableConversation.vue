<template>
    <bm-draggable
        v-if="draggable"
        class="draggable-conversation"
        :class="{ muted: isMuted }"
        :tooltip="tooltip"
        name="conversation"
        :data="conversation"
        disable-touch
        :autoscroll="autoscroll"
        @dragenter="({ relatedData }) => setTooltip(relatedData)"
        @dragleave="resetTooltip"
        @drop="({ relatedData: folder }) => moveConversation(folder)"
        @dragstart="$emit('dragstart', $event)"
        @dragend="$emit('dragend', $event)"
    >
        <conversation-list-item
            :conversation="conversation"
            :is-selected="isSelected"
            :multiple="multiple"
            :selection-mode="selectionMode"
            @check="$emit('check')"
        >
            <template v-slot:actions> <slot name="actions" /> </template>
        </conversation-list-item>
        <template v-slot:shadow>
            <conversation-list-item-shadow :conversation="conversation" :count="shadowCount" />
        </template>
    </bm-draggable>
    <conversation-list-item
        v-else
        :conversation="conversation"
        :is-selected="isSelected"
        :multiple="multiple"
        :selection-mode="selectionMode"
        tabindex="-1"
        @check="$emit('check')"
    >
        <template v-slot:actions> <slot name="actions" /> </template>
    </conversation-list-item>
</template>

<script>
import { BmDraggable } from "@bluemind/styleguide";
import ConversationListItemShadow from "./ConversationListItemShadow";
import ConversationListItem from "./ConversationListItem";
import { MoveMixin, SelectionMixin } from "~/mixins";

export default {
    name: "DraggableConversation",
    components: { ConversationListItemShadow, BmDraggable, ConversationListItem },
    mixins: [MoveMixin, SelectionMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isMuted: {
            type: Boolean,
            required: true
        },
        isSelected: {
            type: Boolean,
            required: true
        },
        draggable: {
            type: Boolean,
            required: true
        },
        multiple: {
            type: Boolean,
            required: true
        },
        selectionMode: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            tooltip: {
                cursor: "cursor",
                text: this.$t("mail.actions.move")
            }
        };
    },
    computed: {
        dragged() {
            return this.isSelected ? this.selected : this.conversation;
        },
        shadowCount() {
            return this.isSelected ? this.selectionLength : 1;
        }
    },
    created() {
        this.autoscroll = {
            container: document.getElementById("folder-sidebar"),
            speed: 500
        };
    },
    methods: {
        moveConversation(folder) {
            this.isValidFolder(folder) && this.MOVE_CONVERSATIONS({ conversations: this.dragged, folder });
        },
        setTooltip(folder) {
            if (folder) {
                if (this.conversation.folderRef.key === folder.key) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.self", {
                        path: folder.path
                    });
                    this.tooltip.cursor = "forbidden";
                } else if (!folder.writable) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.readonly", {
                        path: folder.path
                    });
                    this.tooltip.cursor = "forbidden";
                } else {
                    this.tooltip.text = this.$tc("mail.actions.move.item", this.shadowCount, {
                        path: folder.path
                    });
                    this.tooltip.cursor = "cursor";
                }
            }
        },
        resetTooltip() {
            this.tooltip.text = this.$t("mail.actions.move");
            this.tooltip.cursor = "cursor";
        },
        isValidFolder(folder) {
            return this.conversation.folderRef.key !== folder.key && folder.writable;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.draggable-conversation {
    cursor: pointer;

    &.muted > .conversation-list-item {
        opacity: 0.55;
    }

    &:focus-within,
    &:hover {
        .bm-check {
            display: block !important;
        }
        .bm-avatar,
        .conversation-avatar {
            display: none !important;
        }
    }

    &:focus-visible {
        outline: none !important;
    }
    &:focus,
    &:focus-visible {
        .list-group-item {
            outline: $outline;
            outline-offset: -1px;
        }
    }
}
</style>
