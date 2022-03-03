<template>
    <div class="mail-folder-item-menu">
        <bm-contextual-menu boundary="viewport">
            <bm-dropdown-item-button
                :disabled="!folder.allowSubfolder"
                icon="plus"
                @click.stop.prevent="$emit('create')"
            >
                {{ $t("mail.folder.create_subfolder") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefaultOrMailshareRoot" icon="rename" @click.stop="$emit('edit')">
                {{ $t("mail.folder.rename") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button
                :disabled="isDefaultOrMailshareRoot"
                icon="folder"
                @click.stop="openMoveFolderModal"
            >
                {{ $t("mail.folder.move") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefaultOrMailshareRoot" icon="trash" @click.stop="deleteFolder">
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
        <choose-folder-modal
            ref="move-modal"
            :title="$t('mail.folder.move')"
            :excluded-folders="excludedFolders"
            :included-mailboxes="[mailbox]"
            @ok="moveFolder"
        />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import { isDefault, isMailshareRoot, DEFAULT_FOLDERS } from "~/model/folder";
import { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MAILBOX_TRASH, FOLDER_GET_DESCENDANTS } from "~/getters";
import { EMPTY_FOLDER, MARK_FOLDER_AS_READ, MOVE_FOLDER, REMOVE_FOLDER } from "~/actions";
import { MailRoutesMixin } from "~/mixins";
import ChooseFolderModal from "../ChooseFolderModal";

export default {
    name: "MailFolderItemMenu",
    components: {
        BmContextualMenu,
        BmDropdownItemButton,
        ChooseFolderModal
    },
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            excludedFolders: []
        };
    },
    computed: {
        ...mapGetters("mail", { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MAILBOX_TRASH, FOLDER_GET_DESCENDANTS }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),

        isDefaultOrMailshareRoot() {
            return (
                isDefault(!this.folder.parent, this.folder.imapName, this.mailbox) ||
                isMailshareRoot(this.folder, this.mailbox)
            );
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
        async moveFolder(destinationFolder) {
            this.MOVE_FOLDER({
                folder: this.folder,
                parent: destinationFolder,
                mailbox: this.mailbox
            });
        },
        openMoveFolderModal() {
            const descendantsKeys = this.FOLDER_GET_DESCENDANTS(this.folder).map(child => child.key);
            this.excludedFolders = [this.folder.key, this.folder.parent, ...descendantsKeys];
            this.$refs["move-modal"].show();
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
