<template>
    <bm-draggable
        v-if="!folder.default"
        class="draggable-mail-folder-item flex-fill"
        :tooltip="tooltip"
        disable-touch
        name="folder"
        :data="folder"
        @dragenter="({ relatedData }) => setTooltip(relatedData)"
        @dragleave="resetTooltip"
        @drop="drop()"
        @dragstart="$emit('dragstart', $event)"
        @dragend="$emit('dragend', $event)"
    >
        <mail-folder-item :folder-key="folder.key" />
        <template v-slot:shadow>
            <mail-folder-item-shadow :folder="folder" />
        </template>
    </bm-draggable>
    <mail-folder-item v-else :folder-key="folder.key" />
</template>
<script>
import { BmDraggable } from "@bluemind/styleguide";

import MailFolderItem from "./MailFolderItem";
import MailFolderItemShadow from "./MailFolderItemShadow";
export default {
    name: "DraggableMailFolderItem",
    components: { BmDraggable, MailFolderItem, MailFolderItemShadow },
    props: {
        folder: {
            type: Object,
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
    methods: {
        setTooltip(folder) {
            if (folder) {
                if (this.folder.key === folder.key) {
                    this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.self");
                    this.tooltip.cursor = "forbidden";
                } else if (folder.mailboxRef.key !== this.folder.mailboxRef.key) {
                    this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.other_mailbox");
                    this.tooltip.cursor = "forbidden";
                } else if (!folder.writable) {
                    this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.readonly", {
                        path: folder.path
                    });
                    this.tooltip.cursor = "forbidden";
                } else {
                    this.tooltip.text = this.$t("mail.actions.move_folder.item", { path: folder.path });
                    this.tooltip.cursor = "cursor";
                }
            }
        },
        resetTooltip() {
            this.tooltip.text = this.$t("mail.actions.move");
            this.tooltip.cursor = "cursor";
        },
        isValidFolder(folder) {
            return (
                this.folder.key !== folder.key &&
                folder.writable &&
                folder.mailboxRef.key !== this.folder.mailboxRef.key
            );
        },
        drop() {}
    }
};
</script>
