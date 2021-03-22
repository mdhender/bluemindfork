<template>
    <folder-list-loading v-if="!isLoaded" :name="name" />
    <mail-folder-tree
        v-else
        :tree="MY_MAILBOX_ROOT_FOLDERS"
        :name="name"
        show-input
        @toggle-folders="$emit('toggle-folders')"
    />
</template>
<script>
import { mapGetters } from "vuex";
import { inject } from "@bluemind/inject";
import { MY_MAILBOX, MY_MAILBOX_ROOT_FOLDERS } from "~getters";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
import { LoadingStatus } from "../../model/loading-status";
export default {
    components: { MailFolderTree, FolderListLoading },
    computed: {
        ...mapGetters("mail", { MY_MAILBOX, MY_MAILBOX_ROOT_FOLDERS }),
        isLoaded() {
            return this.MY_MAILBOX && this.MY_MAILBOX.loading === LoadingStatus.LOADED;
        },
        name: () => inject("UserSession").defaultEmail
    }
};
</script>
