<template>
    <bm-draggable
        class="draggable-message"
        :class="{ muted: isMuted }"
        :tooltip="tooltip"
        name="message"
        :data="message"
        disable-touch
        @dragenter="({ relatedData }) => setTooltip(relatedData)"
        @dragleave="resetTooltip"
        @drop="({ relatedData: folder }) => isValidFolder(folder) && MOVE_MESSAGES({ messages: dragged, folder })"
        @dragstart="$emit('dragstart', $event)"
        @dragend="$emit('dragend', $event)"
    >
        <message-list-item :message="message" @toggle-select="$emit('toggle-select', message.key)" />
        <template v-slot:shadow>
            <mail-message-list-item-shadow :message="message" :count="selection.length" />
        </template>
    </bm-draggable>
</template>

<script>
import { BmDraggable } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import MailMessageListItemShadow from "./MailMessageListItemShadow";
import MessageListItem from "./MessageListItem";
import { MESSAGE_IS_SELECTED } from "~getters";
import { MoveMixin } from "~mixins";

export default {
    name: "DraggableMessage",
    components: {
        MailMessageListItemShadow,
        BmDraggable,
        MessageListItem
    },
    mixins: [MoveMixin],
    props: {
        message: {
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
        ...mapState("mail", ["messages", "selection"]),
        ...mapGetters("mail", { MESSAGE_IS_SELECTED }),
        ...mapGetters("mail-webapp", ["nextMessageKey"]),
        dragged() {
            return this.MESSAGE_IS_SELECTED(this.message.key)
                ? this.selection.map(key => this.messages[key])
                : this.message;
        }
    },
    methods: {
        setTooltip(folder) {
            if (folder) {
                if (this.message.folderRef.key === folder.key) {
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
                    this.tooltip.text = this.$tc("mail.actions.move.item", this.selection.length, {
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
            return this.message.folderRef.key !== folder.key && folder.writable;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.draggable-message {
    cursor: pointer;

    &.muted > .message-list-item {
        opacity: 0.55;
    }

    &:focus-within,
    &:hover {
        .bm-check {
            display: block !important;
        }
        .bm-avatar {
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
