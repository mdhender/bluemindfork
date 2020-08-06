<template>
    <bm-contextual-menu
        class="mail-folder-item-menu d-none"
        boundary="viewport"
        @shown="shown = true"
        @hidden="shown = false"
    >
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isReadOnly"
            icon="plus"
            @click.stop.prevent="createSubFolder"
        >
            {{ $t("mail.folder.create.subfolder") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isMailshareRoot || isReadOnly"
            icon="rename"
            @click.stop="$emit('edit')"
        >
            {{ $t("mail.folder.rename") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isMailshareRoot || isReadOnly"
            icon="trash"
            @click.stop="deleteFolder"
        >
            {{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button :disabled="folder.unread === 0" icon="read" @click.stop="markFolderAsRead(folder.key)">
            {{ $t("mail.folder.mark_as_read") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { FolderAdaptor } from "../../store/helpers/FolderAdaptor";

export default {
    name: "MailFolderItemMenu",
    components: {
        BmContextualMenu,
        BmDropdownItemButton
    },
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { hasChildren: "HAS_CHILDREN_GETTER" }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),
        isMailshareRoot() {
            return FolderAdaptor.isMailshareRoot(this.folder, this.mailboxes[this.folder.mailbox]);
        },
        isDefaultFolder() {
            return FolderAdaptor.isMyMailboxDefaultFolder(this.folder);
        },
        isReadOnly() {
            return !this.folder.writable;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["removeFolder", "markFolderAsRead"]),
        ...mapMutations("mail", ["CREATE_FOLDER", "TOGGLE_EDIT_FOLDER", "TOGGLE_FOLDER"]),
        async deleteFolder() {
            const modalTitleKey = this.hasChildren(this.folder.key)
                ? "mail.folder.delete.dialog.question.with_subfolders"
                : "mail.folder.delete.dialog.question";
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t(modalTitleKey, { name: this.folder.name }), {
                title: this.$t("mail.folder.delete.dialog.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false
            });
            if (confirm) {
                const keyBeingRemoved = this.folder.key;
                this.removeFolder(this.folder.key).then(() => {
                    if (this.activeFolder === keyBeingRemoved) {
                        this.$router.push({ name: "mail:home" });
                    }
                });
            }
        },
        async createSubFolder() {
            const mailbox = this.mailboxes[this.folder.mailbox];
            const key = UUIDGenerator.generate();
            this.CREATE_FOLDER({ key, name: "", parent: this.folder.key, mailbox });
            await this.$nextTick();
            // TODO: Remove when new store is complete. mUsing key as uid here is a hack.
            this.TOGGLE_EDIT_FOLDER(key);
            if (!this.folder.expanded) {
                this.TOGGLE_FOLDER(this.folder.key);
            }
        }
    }
};
</script>
