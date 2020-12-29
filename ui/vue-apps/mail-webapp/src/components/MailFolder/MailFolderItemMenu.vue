<template>
    <bm-contextual-menu class="mail-folder-item-menu" boundary="viewport">
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
        <bm-dropdown-item-button
            :disabled="folder.unread === 0"
            icon="read"
            @click.stop="MARK_FOLDER_AS_READ({ folder, mailbox })"
        >
            {{ $t("mail.folder.mark_as_read") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { FolderAdaptor } from "../../store/folders/helpers/FolderAdaptor";
import { create } from "~model/folder";
import { SET_FOLDER_EXPANDED, ADD_FOLDER, TOGGLE_EDIT_FOLDER } from "~mutations";
import { FOLDER_HAS_CHILDREN } from "~getters";
import { MARK_FOLDER_AS_READ, REMOVE_FOLDER } from "~actions";

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
        ...mapGetters("mail", { FOLDER_HAS_CHILDREN }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),
        isMailshareRoot() {
            return FolderAdaptor.isMailshareRoot(this.folder, this.mailbox);
        },
        isDefaultFolder() {
            return FolderAdaptor.isMyMailboxDefaultFolder(this.folder);
        },
        isReadOnly() {
            return !this.folder.writable;
        },
        mailbox() {
            return this.mailboxes[this.folder.mailboxRef.key];
        }
    },
    methods: {
        ...mapActions("mail", { REMOVE_FOLDER, MARK_FOLDER_AS_READ }),
        ...mapMutations("mail", { ADD_FOLDER, TOGGLE_EDIT_FOLDER, SET_FOLDER_EXPANDED }),
        async deleteFolder() {
            const modalTitleKey = this.FOLDER_HAS_CHILDREN(this.folder.key)
                ? "mail.folder.delete.dialog.question.with_subfolders"
                : "mail.folder.delete.dialog.question";
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t(modalTitleKey, { name: this.folder.name }), {
                title: this.$t("mail.folder.delete.dialog.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                const keyBeingRemoved = this.folder.key;
                this.REMOVE_FOLDER({ folder: this.folder, mailbox: this.mailbox });
                if (this.activeFolder === keyBeingRemoved) {
                    this.$router.push({ name: "mail:home" });
                }
            }
        },
        async createSubFolder() {
            const key = UUIDGenerator.generate();
            this.ADD_FOLDER(create(key, "", this.folder, this.mailbox));
            await this.$nextTick();
            // FIXME: FEATWEBML-1386
            this.TOGGLE_EDIT_FOLDER(key);
            this.SET_FOLDER_EXPANDED({ ...this.folder, expanded: true });
        }
    }
};
</script>
