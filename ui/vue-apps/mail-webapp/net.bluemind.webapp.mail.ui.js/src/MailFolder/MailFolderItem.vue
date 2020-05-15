<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="mail-folder-item w-100 d-flex align-items-center"
        @dragenter="folder.expanded || expandFolder(folder.key)"
    >
        <mail-folder-icon
            v-if="!isEditInputOpen"
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <mail-folder-input
            v-else
            ref="folder-input"
            :folder="folder"
            :shared="shared"
            @close="isEditInputOpen = false"
            @submit="rename"
        />
        <bm-contextual-menu
            v-if="folder.writable && !isEditInputOpen"
            class="d-none"
            boundary="viewport"
            @shown="shown = true"
            @hidden="shown = false"
        >
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
import { BmContextualMenu, BmCounterBadge, BmDropdownItemButton, BmDropzone, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { isDefaultFolder } from "@bluemind/backend.mail.store";
import MailFolderIcon from "../MailFolderIcon";
import MailFolderInput from "../MailFolderInput";

export default {
    name: "MailFolderItem",
    components: {
        BmContextualMenu,
        BmCounterBadge,
        BmDropdownItemButton,
        BmDropzone,
        BmIcon,
        MailFolderIcon,
        MailFolderInput
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
            isEditInputOpen: false
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
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder", "removeFolder", "markFolderAsRead", "renameFolder"]),
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
        rename(newFolderName) {
            this.renameFolder({ folderKey: this.folder.key, newFolderName }).then(() => {
                if (this.currentFolderKey === this.folder.key) {
                    this.$router.navigate({ name: "v:mail:message", params: { folder: this.folder.key } });
                }
            });
        },
        openRenameInput() {
            this.isEditInputOpen = true;
            this.$nextTick(() => this.$refs["folder-input"].select());
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
