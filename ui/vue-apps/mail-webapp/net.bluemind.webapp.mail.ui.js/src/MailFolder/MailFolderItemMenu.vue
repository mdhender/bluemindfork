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
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import { isDefaultFolder } from "@bluemind/backend.mail.store";
import { ItemUri } from "@bluemind/item-uri";
import { mapActions, mapGetters, mapMutations } from "vuex";
import UUIDGenerator from "@bluemind/uuid";

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
