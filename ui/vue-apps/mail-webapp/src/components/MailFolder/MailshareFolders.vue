<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="MAILSHARES.length > 0"
        :tree="MAILSHARE_ROOT_FOLDERS"
        :name="$t('common.mailshares')"
    />
</template>
<script>
import { mapGetters } from "vuex";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
import { MAILBOXES_ARE_LOADED, MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~/getters";

export default {
    name: "MailshareFolders",
    components: { MailFolderTree, FolderListLoading },
    computed: {
        ...mapGetters("mail", { MAILSHARE_ROOT_FOLDERS, MAILSHARES, MAILBOXES_ARE_LOADED }),
        isLoaded() {
            return this.MAILBOXES_ARE_LOADED && (!this.MAILSHARES.length || this.MAILSHARE_ROOT_FOLDERS.length);
        }
    }
};
</script>
