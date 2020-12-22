<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="MAILSHARES.length > 0"
        :tree="MAILSHARE_ROOT_FOLDERS"
        :name="$t('common.mailshares')"
        show-input
        @toggle-folders="$emit('toggle-folders')"
    />
</template>
<script>
import { mapGetters, mapState } from "vuex";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
import { MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~getters";

export default {
    name: "MailshareFolders",
    components: { MailFolderTree, FolderListLoading },
    computed: {
        ...mapState("mail", { isLoaded: ({ folderList }) => folderList.mailsharesAreLoaded }),
        ...mapGetters("mail", { MAILSHARE_ROOT_FOLDERS, MAILSHARES })
    }
};
</script>
