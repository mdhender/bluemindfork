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
import { MY_MAILBOX_ROOT_FOLDERS } from "~getters";
import MailFolderTree from "./MailFolderTree.vue";
import FolderListLoading from "./FolderListLoading.vue";
export default {
    components: { MailFolderTree, FolderListLoading },
    inject: ["initialized"],
    data() {
        return { isLoaded_: false };
    },
    computed: {
        ...mapGetters("mail", { MY_MAILBOX_ROOT_FOLDERS }),
        isLoaded() {
            return this.MY_MAILBOX_ROOT_FOLDERS.length > 0 || this.isLoaded_;
        },
        name() {
            return inject("UserSession").defaultEmail;
        }
    },
    async created() {
        await this.initialized;
        this.isLoaded_ = true;
    }
};
</script>
