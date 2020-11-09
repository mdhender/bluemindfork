<template>
    <bm-draggable
        class="draggable-message"
        :class="{ muted: isMuted }"
        :tooltip="tooltip"
        name="message"
        :data="message"
        disable-touch
        @dragenter="e => setTooltip(e.relatedData)"
        @dragleave="resetTooltip"
        @drop="e => moveMessage(e.relatedData)"
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
import { BmTooltip, BmDraggable } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import MailMessageListItemShadow from "./MailMessageListItemShadow";
import MessageListItem from "./MessageListItem";
import { MESSAGE_IS_SELECTED } from "~getters";

export default {
    name: "DraggableMessage",
    components: {
        MailMessageListItemShadow,
        BmDraggable,
        MessageListItem
    },
    directives: { BmTooltip },
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
        ...mapState("mail", ["folders", "selection"]),
        ...mapGetters("mail", { MESSAGE_IS_SELECTED }),
        ...mapGetters("mail-webapp", ["nextMessageKey"])
    },
    methods: {
        ...mapActions("mail-webapp", ["move"]),
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
        moveMessage(folder) {
            if (this.message.folderRef.key !== folder.key && folder.writable) {
                if (this.message.key === this.currentMessageKey) {
                    this.$router.navigate({ name: "v:mail:message", params: { message: this.nextMessageKey } });
                }
                if (this.MESSAGE_IS_SELECTED(this.message.key)) {
                    this.move({ messageKey: this.selection, folder: this.folders[folder.key] });
                } else {
                    this.move({ messageKey: this.message.key, folder: this.folders[folder.key] });
                }
            }
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
