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
        <bm-dropdown-item-button icon="broom" @click.stop="emptyFolder">
            {{ $t("mail.folder.empty") }}
        </bm-dropdown-item-button>
    </bm-contextual-menu>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { BmContextualMenu, BmDropdownItemButton } from "@bluemind/styleguide";
import UUIDGenerator from "@bluemind/uuid";
import { create, isDefault, isMailshareRoot } from "~/model/folder";
import { SET_FOLDER_EXPANDED, ADD_FOLDER, TOGGLE_EDIT_FOLDER } from "~/mutations";
import { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MY_TRASH } from "~/getters";
import { EMPTY_FOLDER, MARK_FOLDER_AS_READ, MOVE_FOLDER } from "~/actions";
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
        ...mapGetters("mail", { IS_DESCENDANT, FOLDER_HAS_CHILDREN, MY_TRASH }),
        ...mapState("mail", ["mailboxes", "folders", "activeFolder"]),
        isMailshareRoot() {
            return isMailshareRoot(this.folder, this.mailbox);
        },
        isDefaultFolder() {
            return isDefault(!this.folder.parent, this.folder.imapName, this.mailbox);
        },
        mailbox() {
            return this.mailboxes[this.folder.mailboxRef.key];
        }
    },
    methods: {
        ...mapActions("mail", { EMPTY_FOLDER, MOVE_FOLDER, MARK_FOLDER_AS_READ }),
        ...mapMutations("mail", { ADD_FOLDER, TOGGLE_EDIT_FOLDER, SET_FOLDER_EXPANDED }),
        async deleteFolder() {
            const modalTitleKey = this.FOLDER_HAS_CHILDREN(this.folder)
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
                if (this.IS_DESCENDANT(this.folder.key, this.activeFolder) || this.activeFolder === this.folder.key) {
                    await this.$router.push({ name: "mail:home" });
                }
                this.MOVE_FOLDER({ folder: this.folder, parent: this.MY_TRASH, mailbox: this.mailbox });
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
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.folder.empty.confirm.content"), {
                title: this.$t("mail.folder.empty"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false,
                autoFocusButton: "ok"
            });
            if (confirm) {
                this.EMPTY_FOLDER({ folder: this.folder, mailbox: this.mailbox });
                this.$router.navigate(this.folderRoute(this.folder));
            }
        }
    }
};
</script>
