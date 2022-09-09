<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="MAILSHARES.length > 0"
        :tree="MAILSHARE_ROOT_FOLDERS"
        :collapsed="collapsed"
        @toggle-tree="toggleTree"
    >
        <template v-slot:title class="folder-tree-header">
            <mail-mailbox-icon :mailbox="MAILSHARES[0]" class="folder-tree-avatar" />
            <div class="folder-tree-name">{{ $t("common.mailshares") }}</div>
        </template>
    </mail-folder-tree>
</template>
<script>
import { mapGetters } from "vuex";
import FolderListLoading from "./FolderListLoading";
import MailFolderTree from "./MailFolderTree";
import MailMailboxIcon from "../MailMailboxIcon";
import { MAILBOXES_ARE_LOADED, MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~/getters";
import { FolderTreeMixin } from "~/mixins";

export default {
    name: "MailshareFolders",
    components: { MailMailboxIcon, MailFolderTree, FolderListLoading },
    mixins: [FolderTreeMixin],
    data() {
        return { treeKey: "mailshares-tree" };
    },
    computed: {
        ...mapGetters("mail", { MAILSHARE_ROOT_FOLDERS, MAILSHARES, MAILBOXES_ARE_LOADED }),
        isLoaded() {
            return this.MAILBOXES_ARE_LOADED && (!this.MAILSHARES.length || this.MAILSHARE_ROOT_FOLDERS.length);
        }
    }
};
</script>
