<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree
        v-else-if="MAILSHARES.length > 0"
        :tree="MAILSHARE_ROOT_FOLDERS"
        :name="$t('common.mailshares')"
        @toggle-folders="$emit('toggle-folders')"
    />
</template>
<script>
import { mapGetters } from "vuex";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
import { MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~getters";

export default {
    name: "MailshareFolders",
    components: { MailFolderTree, FolderListLoading },
    inject: ["initialized"],
    data() {
        return { isLoaded_: false };
    },
    computed: {
        ...mapGetters("mail", { MAILSHARE_ROOT_FOLDERS, MAILSHARES }),
        isLoaded() {
            return this.MAILSHARES.length > 0 || this.isLoaded_;
        }
    },
    async created() {
        await this.initialized;
        this.isLoaded_ = true;
    }
};
</script>
