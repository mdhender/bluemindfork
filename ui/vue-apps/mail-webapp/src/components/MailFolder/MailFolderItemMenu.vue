<template>
    <bm-contextual-menu class="mail-folder-item-menu" boundary="viewport">
        <bm-dropdown-item-button :disabled="!folder.allowSubfolder" icon="plus" @click.stop.prevent="createSubFolder">
            {{ $t("mail.folder.create.subfolder") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isMailshareRoot"
            icon="rename"
            @click.stop="$emit('edit')"
        >
            {{ $t("mail.folder.rename") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button :disabled="isDefaultFolder || isMailshareRoot" icon="trash" @click.stop="deleteFolder">
            {{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="folder.unread === 0"
            icon="read"
            @click.stop="MARK_FOLDER_AS_READ({ folder, mailbox })"
        >
            {{ $t("mail.folder.mark_as_read") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button v-if="isTrash" icon="broom" @click.stop="emptyTrash">
            {{ $t("mail.actions.empty_trash.label") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button v-else icon="broom" @click.stop="emptyFolder">
            {{ $t("mail.actions.empty_folder.label") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { create, isDefault, isMailshareRoot, DEFAULT_FOLDERS } from "~/model/folder";
import { SET_FOLDER_EXPANDED, ADD_FOLDER, TOGGLE_EDIT_FOLDER } from "~/mutations";
import { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MAILBOX_TRASH } from "~/getters";
import { EMPTY_FOLDER, MARK_FOLDER_AS_READ, MOVE_FOLDER, REMOVE_FOLDER } from "~/actions";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MailFolderItemMenu",
    components: {
        BmContextualMenu,
        BmDropdownItemButton
    },
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MAILBOX_TRASH }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),
        isMailshareRoot() {
            return isMailshareRoot(this.folder, this.mailbox);
        },
        isDefaultFolder() {
            return isDefault(!this.folder.parent, this.folder.imapName, this.mailbox);
        },
        isTrash() {
            return this.isDefaultFolder && this.folder.imapName === DEFAULT_FOLDERS.TRASH;
        },
        hasChildren() {
            return this.FOLDER_HAS_CHILDREN(this.folder);
        },
        mailbox() {
            return this.mailboxes[this.folder.mailboxRef.key];
        }
    },
    methods: {
        ...mapActions("mail", { EMPTY_FOLDER, MOVE_FOLDER, MARK_FOLDER_AS_READ, REMOVE_FOLDER }),
        ...mapMutations("mail", { ADD_FOLDER, TOGGLE_EDIT_FOLDER, SET_FOLDER_EXPANDED }),
        async deleteFolder() {
            const trash = this.MAILBOX_TRASH(this.mailbox);
            const remove = this.IS_DESCENDANT(trash.key, this.folder.key);
            const prefix = `mail.actions.${remove ? "remove_folder" : "move_folder_to_trash"}.modal`;
            const title = this.$t(`${prefix}.title`);
            const content = this.$t(`${prefix}.content.${this.hasChildren ? "with_subfolder" : "without_subfolder"}`, {
                name: this.folder.name
            });
            const confirm = await this.confirm(title, content);
            if (confirm) {
                if (this.IS_DESCENDANT(this.folder.key, this.activeFolder) || this.activeFolder === this.folder.key) {
                    await this.$router.push({ name: "mail:home" });
                }
                if (remove) {
                    this.REMOVE_FOLDER({ folder: this.folder, mailbox: this.mailbox });
                } else {
                    this.MOVE_FOLDER({
                        folder: this.folder,
                        parent: trash,
                        mailbox: this.mailbox
                    });
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
        },
        async emptyFolder() {
            const confirm = await this.confirm(
                this.$t("mail.actions.empty_folder.modal.title"),
                this.$t("mail.actions.empty_folder.modal.content", { name: this.folder.name })
            );
            if (confirm) {
                this.EMPTY_FOLDER({ folder: this.folder, mailbox: this.mailbox });
                this.$router.navigate(this.folderRoute(this.folder));
            }
        },
        async emptyTrash() {
            const confirm = await this.confirm(
                this.$t("mail.actions.empty_trash.modal.title"),
                this.$t(`mail.actions.empty_trash.modal.content.${this.hasChildren ? "with" : "without"}_subfolder`)
            );
            if (confirm) {
                this.EMPTY_FOLDER({ folder: this.folder, mailbox: this.mailbox, deep: true });
                this.$router.navigate(this.folderRoute(this.folder));
            }
        },
        confirm(title, content) {
            return this.$bvModal.msgBoxConfirm(content, {
                title,
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
        }
    }
};
</script>
