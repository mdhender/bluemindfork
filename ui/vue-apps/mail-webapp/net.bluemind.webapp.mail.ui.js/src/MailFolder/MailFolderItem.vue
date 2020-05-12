<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="mail-folder-item w-100 d-flex align-items-center"
        :class="computeEditBorder"
        @dragenter="folder.expanded || expandFolder(folder.key)"
    >
        <mail-folder-icon
            v-if="!isEditInputOpen"
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <div v-else :class="isNewFolderNameValid === true ? 'valid' : 'invalid'" class="edit flex-fill">
            <bm-icon :icon="shared ? 'folder-shared' : 'folder'" />
            <bm-form-input
                ref="edit"
                v-model="newFolderName"
                type="text"
                class="d-inline-block w-100"
                reset
                @focusout="onInputFocusOut"
                @keydown.enter="edit"
                @keydown.esc="cancelEdit"
                @keydown.left.stop
                @keydown.right.stop
                @mousedown.stop
                @reset="isEditInputOpen = false"
            />
            <bm-notice
                v-if="isNewFolderNameValid !== true"
                :text="isNewFolderNameValid"
                class="position-absolute w-75"
            />
        </div>
        <bm-contextual-menu v-if="!isEditInputOpen" class="d-none" boundary="viewport">
            <bm-dropdown-item-button :disabled="isDefaultFolderOrMailshareRootOrReadOnly" @click.stop="deleteFolder">
                <bm-icon class="mr-2" icon="trash" />{{ $t("common.delete") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="isDefaultFolderOrMailshareRootOrReadOnly" @click.stop="openRenameInput">
                <bm-icon class="mr-2" icon="rename" />{{ $t("mail.folder.rename") }}
            </bm-dropdown-item-button>
            <bm-dropdown-item-button :disabled="folder.unread === 0" @click.stop="markFolderAsRead(folder.key)">
                <bm-icon class="mr-2" icon="read" />{{ $t("mail.folder.mark_as_read") }}
            </bm-dropdown-item-button>
        </bm-contextual-menu>
        <bm-counter-badge
            v-if="folder.unread > 0"
            :value="folder.unread"
            :variant="folder.key != currentFolderKey ? 'secondary' : 'primary'"
            class="mr-1 d-block"
        />
    </bm-dropzone>
</template>

<script>
import {
    BmContextualMenu,
    BmCounterBadge,
    BmDropdownItemButton,
    BmDropzone,
    BmFormInput,
    BmIcon,
    BmNotice
} from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { isDefaultFolder, isFolderNameValid } from "@bluemind/backend.mail.store";
import ItemUri from "@bluemind/item-uri";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailFolderItem",
    components: {
        BmContextualMenu,
        BmCounterBadge,
        BmDropdownItemButton,
        BmDropzone,
        BmFormInput,
        BmIcon,
        BmNotice,
        MailFolderIcon
    },
    props: {
        folder: {
            type: Object,
            required: true
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            isEditInputOpen: false,
            newFolderName: this.folder.name
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapGetters("mail-webapp", ["mailshares", "my"]),
        ...mapGetters("mail-webapp/folders", ["getFolderByPath"]),
        isDefaultFolderOrMailshareRootOrReadOnly() {
            return isDefaultFolder(this.folder) || this.isMailshareRoot || this.folder.writable;
        },
        isMailshareRoot() {
            return (
                !this.folder.parent &&
                this.mailshares.some(mailshare =>
                    mailshare.folders.some(({ uid }) => uid.toUpperCase() === this.folder.uid.toUpperCase())
                )
            );
        },
        computeEditBorder() {
            if (this.isEditInputOpen) {
                if (this.isNewFolderNameValid === true) {
                    return "border-bottom border-primary";
                } else {
                    return "border-bottom border-danger";
                }
            }
            return "";
        },
        isNewFolderNameValid() {
            if (this.newFolderName !== "" && this.newFolderName !== this.folder.name) {
                const currentMailbox = ItemUri.container(this.folder.key);

                const currentFolderName = this.newFolderName.toLowerCase();
                const checkValidity = isFolderNameValid(currentFolderName);
                if (checkValidity !== true) {
                    return this.$t("mail.actions.create.folder.invalid.character", {
                        character: checkValidity
                    });
                }

                let path =
                    this.folder.fullName.substring(0, this.folder.fullName.lastIndexOf("/") + 1) + this.newFolderName;
                const isMailshare = this.my.mailboxUid !== currentMailbox;
                if (isMailshare) {
                    const mailshareName = this.mailshares.find(mailshare => mailshare.uid === currentMailbox).name;
                    path = mailshareName + "/" + path;
                }
                if (this.getFolderByPath(path, currentMailbox)) {
                    return this.$t("mail.actions.create.folder.invalid.already_exist");
                }
            }
            return true;
        }
    },
    watch: {
        folder() {
            this.newFolderName = this.folder.name;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder", "removeFolder", "renameFolder", "markFolderAsRead"]),
        async deleteFolder() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t("mail.folder.delete.dialog.question", { name: this.folder.fullName }),
                {
                    title: this.$t("mail.folder.delete.dialog.title"),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false
                }
            );
            if (confirm) {
                this.removeFolder(this.folder.key).then(() => {
                    if (this.currentFolderKey === this.folder.key) {
                        this.$router.push({ name: "mail:home" });
                    }
                });
            }
        },
        openRenameInput() {
            this.isEditInputOpen = true;
            this.$nextTick(() => this.$refs["edit"].select());
        },
        cancelEdit() {
            this.isEditInputOpen = false;
            this.newFolderName = this.folder.name;
        },
        edit() {
            if (this.isNewFolderNameValid === true && this.newFolderName !== "") {
                if (this.newFolderName !== this.folder.name) {
                    this.renameFolder({ folderKey: this.folder.key, newFolderName: this.newFolderName }).then(() => {
                        if (this.currentFolderKey === this.folder.key) {
                            this.$router.navigate({ name: "v:mail:message", params: { folder: this.folder.key } });
                        }
                    });
                }
                this.isEditInputOpen = false;
            }
        },
        onInputFocusOut() {
            if (!this.$el.contains(document.activeElement) && !this.$el.contains(event.relatedTarget)) {
                if (this.isNewFolderNameValid !== true || this.newFolderName === "") {
                    this.cancelEdit();
                } else {
                    this.edit();
                }
            }
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-item {
    .bm-contextual-menu,
    .bm-counter-badge {
        right: 0;
    }

    .mail-folder-icon {
        padding: {
            top: $sp-1;
            bottom: $sp-1;
        }
    }

    .bm-contextual-menu.d-flex + .bm-counter-badge {
        display: none !important;
    }

    .edit {
        &.valid .fa-folder,
        &.valid .fa-folder-shared {
            color: $primary;
        }

        &.invalid {
            .fa-folder,
            .fa-folder-shared,
            input {
                color: $danger;
            }
        }

        input {
            border: none !important;
            background-color: transparent !important;
        }
    }
}

@include media-breakpoint-up(lg) {
    .mail-folder-item:hover {
        .bm-contextual-menu {
            display: flex !important;
        }
        .bm-counter-badge {
            display: none !important;
        }
    }
}
</style>
