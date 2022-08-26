<template>
    <div class="mail-folder-item-menu d-flex justify-content-center h-100" @click.stop>
        <bm-contextual-menu boundary="viewport" class="flex-fill" v-on="$listeners">
            <bm-dropdown-item-button :disabled="!folder.allowSubfolder" icon="plus" @click="$emit('create')">
                {{ $t("mail.folder.create_subfolder") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefault" icon="rename" @click="$emit('edit')">
                {{ $t("mail.folder.rename") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefault" icon="folder" @click="openMoveFolderModal">
                {{ $t("mail.folder.move") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefault" icon="trash" @click="deleteFolder">
                {{ $t("common.delete") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button
                :disabled="folder.unread === 0"
                icon="read"
                @click="MARK_FOLDER_AS_READ({ folder, mailbox })"
            >
                {{ $t("mail.folder.mark_as_read") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-if="isTrash" icon="broom" @click="emptyTrash">
                {{ $t("mail.actions.empty_trash.label") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button v-else icon="broom" @click="emptyFolder">
                {{ $t("mail.actions.empty_folder.label") }}
            </bm-dropdown-item-button>
        </bm-contextual-menu>
        <choose-folder-modal
            ref="move-modal"
            :ok-title="$t('mail.folder.move')"
            :cancel-title="$t('common.cancel')"
            :title="$t('mail.actions.move_folder.title', { name: folder.name })"
            :is-excluded="isExcluded"
            :mailboxes="[mailbox]"
            :default-folders="defaultFolders"
            @ok="moveFolder"
        />
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import { folderUtils } from "@bluemind/mail";
import { IS_DESCENDANT, FOLDER_BY_PATH, FOLDER_HAS_CHILDREN, MAILBOX_TRASH } from "~/getters";
import { CREATE_FOLDER, EMPTY_FOLDER, MARK_FOLDER_AS_READ, MOVE_FOLDER, REMOVE_FOLDER } from "~/actions";
import { MailRoutesMixin } from "~/mixins";
import ChooseFolderModal from "../ChooseFolderModal";

const {
    createRoot,
    DEFAULT_FOLDERS,
    folderExists,
    getInvalidCharacter,
    isDefault,
    isDescendantPath,
    isRoot
} = folderUtils;

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
            folderParent: null,
            defaultFolders: []
        };
    },
    computed: {
        ...mapGetters("mail", { IS_DESCENDANT, FOLDER_BY_PATH, FOLDER_HAS_CHILDREN, MAILBOX_TRASH }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),
        isDefault() {
            return isDefault(!this.folder.parent, this.folder.imapName, this.mailbox);
        },
        isTrash() {
            return this.isDefault && this.folder.imapName === DEFAULT_FOLDERS.TRASH;
        },
        hasChildren() {
            return this.FOLDER_HAS_CHILDREN(this.folder);
        },
        mailbox() {
            return this.mailboxes[this.folder.mailboxRef.key];
        }
    },
    methods: {
        ...mapActions("mail", { CREATE_FOLDER, EMPTY_FOLDER, MOVE_FOLDER, MARK_FOLDER_AS_READ, REMOVE_FOLDER }),
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
            const folder = { ...this.folder };
            let destination;
            if (isRoot(destinationFolder)) {
                destination = null;
            } else {
                destination = destinationFolder;
                if (!folderExists(destination.path, path => this.FOLDER_BY_PATH(path, this.mailbox))) {
                    destination = await this.CREATE_FOLDER({
                        name: destinationFolder.path,
                        parent: null,
                        mailbox: this.mailbox
                    });
                }
            }
            this.MOVE_FOLDER({ folder, parent: destination, mailbox: this.mailbox });
        },
        openMoveFolderModal() {
            const root = createRoot(this.mailbox);

            const trash = this.$store.getters[`mail/${MAILBOX_TRASH}`](this.mailbox);
            this.defaultFolders = this.folder.parent ? [root, trash] : [trash];

            this.$refs["move-modal"].show();
        },
        confirm(title, content) {
            return this.$bvModal.msgBoxConfirm(content, {
                title,
                okTitle: this.$t("common.delete"),
                cancelTitle: this.$t("common.cancel"),
                okVariant: "secondary",
                cancelVariant: "simple-neutral",
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
        },
        isExcluded(folder) {
            if (folder) {
                if (folder.path === this.folder.path) {
                    return this.$t("mail.actions.move_folder.excluded_folder.same");
                }
                if (isDescendantPath(folder.path, this.folder.path)) {
                    return this.$t("mail.actions.move_folder.excluded_folder.descendant");
                }
                if (folder.path === this.folders[this.folder.parent]?.path) {
                    return this.$t("mail.actions.move_folder.excluded_folder.parent");
                }
                const invalidCharacter = getInvalidCharacter(folder.path);
                if (invalidCharacter) {
                    return this.$t("common.invalid.character", {
                        character: invalidCharacter
                    });
                }
            }
            return false;
        }
    }
};
</script>
