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
import { mapGetters, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { MY_MAILBOX_ROOT_FOLDERS } from "~getters";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
export default {
    components: { MailFolderTree, FolderListLoading },
    computed: {
        ...mapState("mail", { isLoaded: ({ folderList }) => folderList.myMailboxIsLoaded }),
        ...mapGetters("mail", { MY_MAILBOX_ROOT_FOLDERS }),
        name() {
            return inject("UserSession").defaultEmail;
        }
    }
};
</script>
