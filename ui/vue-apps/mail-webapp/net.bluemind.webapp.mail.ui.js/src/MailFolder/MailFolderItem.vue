<template>
    <bm-dropzone
        :states="{ active: false }"
        :accept="['message']"
        :value="folder"
        class="mail-folder-item w-100 d-flex align-items-center"
        @dragenter="folder.expanded || expandFolder(folder.key)"
    >
        <mail-folder-icon
            :shared="shared"
            :folder="folder"
            class="flex-fill"
            :class="folder.unread > 0 ? 'font-weight-bold' : ''"
        />
        <bm-contextual-menu
            v-if="folder.writable"
            class="d-none"
            boundary="viewport"
            @shown="shown = true"
            @hidden="shown = false"
        >
            <bm-dropdown-item-button :disabled="isDefaultFolderOrMailshareRoot" @click.stop="deleteFolder">
                <bm-icon class="mr-2" icon="trash" />{{ $t("common.delete") }}
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
import { BmContextualMenu, BmDropdownItemButton, BmCounterBadge, BmDropzone, BmIcon } from "@bluemind/styleguide";
import { mapActions, mapState, mapGetters } from "vuex";
import MailFolderIcon from "../MailFolderIcon";
import { isDefaultFolder } from "@bluemind/backend.mail.store";

export default {
    name: "MailFolderItem",
    components: {
        BmContextualMenu,
        BmCounterBadge,
        BmDropdownItemButton,
        BmDropzone,
        BmIcon,
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

    computed: {
        ...mapState("mail-webapp", ["currentFolderKey"]),
        ...mapGetters("mail-webapp", ["mailshares"]),
        isDefaultFolderOrMailshareRoot() {
            return isDefaultFolder(this.folder) || this.isMailshareRoot(this.folder);
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder", "removeFolder"]),
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
                this.removeFolder(this.folder.key);
            }
        },
        isMailshareRoot(folder) {
            return (
                !folder.parent &&
                this.mailshares.some(mailshare =>
                    mailshare.folders.some(({ uid }) => uid.toUpperCase() === folder.uid.toUpperCase())
                )
            );
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
