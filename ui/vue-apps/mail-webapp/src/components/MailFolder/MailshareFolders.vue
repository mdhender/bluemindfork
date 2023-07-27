<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="MAILSHARES.length > 0"
        :tree="MAILSHARE_ROOT_FOLDERS"
        :collapsed="collapsed"
        @toggle-tree="toggleTree"
    >
        <template #title>
            <folder-tree-header :mailbox="MAILSHARES[0]" :name="$t('common.mailshares')" />
        </template>
    </mail-folder-tree>
</template>
<script>
import { mapGetters } from "vuex";
import { FolderTreeMixin } from "~/mixins";
import { MAILBOXES_ARE_LOADED, MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~/getters";
import FolderListLoading from "./FolderListLoading";
import MailFolderTree from "./MailFolderTree";
import FolderTreeHeader from "./FolderTreeHeader.vue";

export default {
    name: "MailshareFolders",
    components: { MailFolderTree, FolderListLoading, FolderTreeHeader },
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
