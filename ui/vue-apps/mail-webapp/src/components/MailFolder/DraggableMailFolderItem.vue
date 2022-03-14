<template>
    <bm-draggable
        v-if="!folder.default"
        class="draggable-mail-folder-item flex-fill"
        :tooltip="tooltip"
        disable-touch
        name="folder"
        :data="folder"
        @dragenter="({ relatedData: folder }) => setTooltip(folder)"
        @dragleave="resetTooltip"
        @drop="({ relatedData: folder }) => moveFolder(folder)"
    >
        <mail-folder-item :folder-key="folder.key" />
        <template v-slot:shadow>
            <mail-folder-item-shadow :folder="folder" />
        </template>
    </bm-draggable>
    <mail-folder-item v-else :folder-key="folder.key" />
</template>
<script>
import { mapActions, mapState, mapGetters, mapMutations } from "vuex";
import { BmDraggable } from "@bluemind/styleguide";
import { MOVE_FOLDER } from "~/actions";
import { SET_FOLDER_EXPANDED } from "~/mutations";
import { IS_DESCENDANT, FOLDER_HAS_CHILDREN } from "~/getters";
import { isRoot } from "~/model/folder";
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
    computed: {
        ...mapState("mail", ["mailboxes", "folders"]),
        ...mapGetters("mail", { IS_DESCENDANT, FOLDER_HAS_CHILDREN }),

        mailbox() {
            return this.mailboxes[this.folder.mailboxRef.key];
        }
    },
    methods: {
        ...mapActions("mail", { MOVE_FOLDER }),
        ...mapMutations("mail", { SET_FOLDER_EXPANDED }),

        moveFolder(destination) {
            if (this.isValidFolder(destination)) {
                if (isRoot(destination)) {
                    destination = null;
                }

                this.MOVE_FOLDER({
                    folder: this.folder,
                    parent: destination,
                    mailbox: this.mailbox
                });
            }
        },

        setTooltip(folder) {
            if (folder) {
                if (!this.isValidFolder(folder)) {
                    this.tooltip.cursor = "forbidden";

                    if (this.folder.key === folder.key) {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.self", {
                            name: folder.name
                        });
                    } else if (!folder || folder.mailboxRef.key !== this.folder.mailboxRef.key) {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.other_mailbox");
                    } else if (!folder.writable) {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.readonly", {
                            path: folder.path
                        });
                    } else if (this.IS_DESCENDANT(this.folder.key, folder.key)) {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.child", {
                            path: folder.path
                        });
                    } else if (this.folder.parent === folder.key) {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.warning.parent", {
                            name: folder.name
                        });
                    }
                } else {
                    if (folder.path === "") {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item.to_root", {
                            mailbox: this.mailbox.name
                        });
                    } else {
                        this.tooltip.text = this.$t("mail.actions.move_folder.item", { name: folder.name });
                    }

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
                folder.mailboxRef.key === this.folder.mailboxRef.key &&
                !this.IS_DESCENDANT(this.folder.key, folder.key) &&
                this.folder.parent !== folder.key
            );
        }
    }
};
</script>
