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
        @dragenter="setTooltip"
        @dragleave="resetTooltip"
        @drop="drop"
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
import { ERROR } from "@bluemind/alert.store";
import { folderUtils, messageUtils } from "@bluemind/mail";
import { BmDraggable } from "@bluemind/styleguide";
import { AddAttachmentsCommand } from "~/commands";
import { MoveMixin, SelectionMixin } from "~/mixins";
import apiMessages from "~/store/api/apiMessages";
import { CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import ConversationListItemShadow from "./ConversationListItemShadow";
import ConversationListItem from "./ConversationListItem";

const { translatePath } = folderUtils;

export default {
    name: "DraggableConversation",
    components: { ConversationListItemShadow, BmDraggable, ConversationListItem },
    mixins: [AddAttachmentsCommand, MoveMixin, SelectionMixin],
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
        async attachTo(message) {
            const messages = this.$store.getters[`mail/${CONVERSATION_MESSAGE_BY_KEY}`](this.conversation.key);
            const lastMessage = messages[messages.length - 1];
            try {
                const content = await apiMessages.fetchComplete(lastMessage);
                const file = new File(
                    [content],
                    messageUtils.createEmlName(lastMessage, this.$t("mail.viewer.no.subject")),
                    { type: "message/rfc822" }
                );
                this.$execute("add-attachments", { files: [file], message, maxSize: this.maxSize });
            } catch {
                this.$store.dispatch(`alert/${ERROR}`, {
                    alert: { name: "mail.attach_eml.fetch", uid: "ATTACH_EML_UID" }
                });
            }
        },
        moveConversation(folder) {
            this.isValidFolder(folder) && this.MOVE_CONVERSATIONS({ conversations: this.dragged, folder });
        },
        setTooltip(event) {
            if (event.relatedData?.path) {
                const folder = event.relatedData;
                const path = translatePath(folder.path);
                if (this.conversation.folderRef.key === folder.key) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.self", { path });
                    this.tooltip.cursor = "forbidden";
                } else if (!folder.writable) {
                    this.tooltip.text = this.$t("mail.actions.move.item.warning.readonly", { path });
                    this.tooltip.cursor = "forbidden";
                } else {
                    this.tooltip.text = this.$tc("mail.actions.move.item", this.shadowCount, { path });
                    this.tooltip.cursor = "cursor";
                }
            } else if (event.relatedData?.folderRef) {
                this.tooltip.text = this.$t("mail.actions.attach");
                this.tooltip.cursor = "cursor";
            }
        },
        resetTooltip() {
            this.tooltip.text = this.$t("mail.actions.move");
            this.tooltip.cursor = "cursor";
        },
        isValidFolder(folder) {
            return this.conversation.folderRef.key !== folder.key && folder.writable;
        },
        drop(event) {
            const relatedData = event.relatedData;
            if (relatedData?.path) {
                this.moveConversation(event.relatedData);
            } else if (relatedData?.folderRef) {
                this.attachTo(event.relatedData);
            }
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
.draggable-conversation {
    cursor: pointer;

    &.muted .conversation-list-item {
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
