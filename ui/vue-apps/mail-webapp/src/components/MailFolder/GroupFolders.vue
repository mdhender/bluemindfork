<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="GROUP_MAILBOXES.length > 0"
        :tree="GROUP_MAILBOX_ROOT_FOLDERS"
        :collapsed="collapsed"
        @toggle-tree="toggleTree"
    >
        <template v-slot:title>
            <div class="folder-tree-header">
                <mail-mailbox-icon :mailbox="GROUP_MAILBOXES[0]" class="folder-tree-avatar" />
                <div class="folder-tree-name">{{ $t("mail.folders.groups") }}</div>
            </div>
        </template>
    </mail-folder-tree>
</template>
<script>
import { mapGetters } from "vuex";
import FolderListLoading from "./FolderListLoading";
import MailFolderTree from "./MailFolderTree";
import MailMailboxIcon from "../MailMailboxIcon";
import { MAILBOXES_ARE_LOADED, GROUP_MAILBOX_ROOT_FOLDERS, GROUP_MAILBOXES } from "~/getters";
import { FolderTreeMixin } from "~/mixins";

export default {
    name: "GroupFolders",
    components: { MailMailboxIcon, MailFolderTree, FolderListLoading },
    mixins: [FolderTreeMixin],
    data() {
        return { treeKey: "mailshares-tree" };
    },
    computed: {
        ...mapGetters("mail", { GROUP_MAILBOX_ROOT_FOLDERS, GROUP_MAILBOXES, MAILBOXES_ARE_LOADED }),
        isLoaded() {
            return (
                this.MAILBOXES_ARE_LOADED && (!this.GROUP_MAILBOXES.length || this.GROUP_MAILBOX_ROOT_FOLDERS.length)
            );
        }
    }
};
</script>
