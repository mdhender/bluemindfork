<template>
    <bm-draggable
        class="draggable-conversation"
        :class="{ muted: isMuted }"
        :tooltip="tooltip"
        name="conversation"
        :data="conversation"
        disable-touch
        @dragenter="({ relatedData }) => setTooltip(relatedData)"
        @dragleave="resetTooltip"
        @drop="
            ({ relatedData: folder }) => isValidFolder(folder) && MOVE_CONVERSATIONS({ conversations: dragged, folder })
        "
        @dragstart="$emit('dragstart', $event)"
        @dragend="$emit('dragend', $event)"
    >
        <conversation-list-item
            :conversation="conversation"
            @toggle-select="$emit('toggle-select', conversation.key)"
        />
        <template v-slot:shadow>
            <mail-conversation-list-item-shadow :conversation="conversation" :count="SELECTION_KEYS.length" />
        </template>
    </bm-draggable>
</template>

<script>
import { BmDraggable } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import MailConversationListItemShadow from "./MailConversationListItemShadow";
import ConversationListItem from "./ConversationListItem";
import { CONVERSATION_IS_SELECTED, SELECTION, SELECTION_KEYS } from "~/getters";
import { MoveMixin } from "~/mixins";

export default {
    name: "DraggableConversation",
    components: {
        MailConversationListItemShadow,
        BmDraggable,
        ConversationListItem
    },
    mixins: [MoveMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isMuted: {
            type: Boolean,
            required: false,
            default: false
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
        ...mapGetters("mail", { CONVERSATION_IS_SELECTED, SELECTION, SELECTION_KEYS }),
        dragged() {
            return this.CONVERSATION_IS_SELECTED(this.conversation.key) ? this.SELECTION : this.conversation;
        }
    },
    methods: {
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
                    this.tooltip.text = this.$tc("mail.actions.move.item", this.SELECTION_KEYS.length, {
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

    &:focus .list-group-item {
        outline: $outline;
        outline-offset: -1px;
        &:hover {
            background-color: $component-active-bg-darken;
        }
    }

    &:focus &:hover {
        background-color: $component-active-bg-darken;
    }
}
</style>
