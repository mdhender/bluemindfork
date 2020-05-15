<template>
    <bm-contextual-menu
        class="mail-folder-item-menu d-none"
        boundary="viewport"
        @shown="shown = true"
        @hidden="shown = false"
    >
        <bm-dropdown-item-button :disabled="isDefaultFolder || isReadOnly" @click.stop.prevent="createSubFolder">
            <bm-icon class="mr-2" icon="plus" />{{ $t("mail.folder.create.subfolder") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isMailshareRoot || isReadOnly"
            @click.stop="$emit('edit')"
        >
            <bm-icon class="mr-2" icon="rename" />{{ $t("mail.folder.rename") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button
            :disabled="isDefaultFolder || isMailshareRoot || isReadOnly"
            @click.stop="deleteFolder"
        >
            <bm-icon class="mr-2" icon="trash" />{{ $t("common.delete") }}
        </bm-dropdown-item-button>
        <bm-dropdown-item-button :disabled="folder.unread === 0" @click.stop="markFolderAsRead(folder.key)">
            <bm-icon class="mr-2" icon="read" />{{ $t("mail.folder.mark_as_read") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { BmContextualMenu, BmDropdownItemButton, BmIcon } from "@bluemind/styleguide";
import { isDefaultFolder } from "@bluemind/backend.mail.store";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapGetters, mapMutations } from "vuex";
import UUIDGenerator from "@bluemind/uuid";

export default {
    name: "MailFolderItemMenu",
    components: {
        BmContextualMenu,
        BmDropdownItemButton,
        BmIcon
    },
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail-webapp", ["mailshares"]),
        isDefaultFolder() {
            return isDefaultFolder(this.folder);
        },
        isMailshareRoot() {
            return (
                !this.folder.parent &&
                this.mailshares.some(mailshare =>
                    mailshare.folders.some(({ uid }) => uid.toUpperCase() === this.folder.uid.toUpperCase())
                )
            );
        },
        isReadOnly() {
            return !this.folder.writable;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["removeFolder", "markFolderAsRead"]),
        ...mapMutations("mail-webapp/folders", { addFolder: "storeItems" }),
        ...mapMutations("mail-webapp", ["expandFolder", "toggleEditFolder"]),
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
        async createSubFolder() {
            const subFolderUid = UUIDGenerator.generate();
            const subFolder = {
                value: {
                    name: "",
                    fullName: this.folder.fullName + "/",
                    path: this.folder.fullName + "/",
                    parentUid: this.folder.uid
                },
                uid: subFolderUid,
                displayName: ""
            };

            this.addFolder({ items: [subFolder], mailboxUid: ItemUri.container(this.folder.key) });
            await this.$nextTick();
            this.toggleEditFolder(subFolderUid);
            this.expandFolder(this.folder.uid);
        }
    }
};
</script>
