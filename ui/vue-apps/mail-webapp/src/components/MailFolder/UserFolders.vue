<template>
    <mail-folder-tree v-if="isLoaded" :tree="MAILBOX_ROOT_FOLDERS(mailbox)" :name="mailbox.name">
        <mail-folder-input class="pl-4" :mailbox-key="mailbox.key" @submit="name => add(name, mailbox)" />
    </mail-folder-tree>
    <folder-list-loading v-else :name="mailbox.name" />
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { MAILBOX_ROOT_FOLDERS } from "~/getters";
import { CREATE_FOLDER } from "~/actions";
import MailFolderTree from "./MailFolderTree";
import FolderListLoading from "./FolderListLoading";
import MailFolderInput from "../MailFolderInput";
import { LoadingStatus } from "~/model/loading-status";
export default {
    name: "UserFolders",
    components: { MailFolderInput, MailFolderTree, FolderListLoading },
    props: {
        mailbox: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { MAILBOX_ROOT_FOLDERS }),
        isLoaded() {
            return this.mailbox && this.mailbox.loading === LoadingStatus.LOADED;
        }
    },
    methods: {
        ...mapActions("mail", { CREATE_FOLDER }),
        add(name, mailbox) {
            this.CREATE_FOLDER({ name, parent: null, mailbox: mailbox });
        }
    }
};
</script>
