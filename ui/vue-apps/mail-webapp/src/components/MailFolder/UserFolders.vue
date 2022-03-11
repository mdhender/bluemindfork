<template>
    <mail-folder-tree v-if="isLoaded" :tree="MAILBOX_ROOT_FOLDERS(mailbox)">
        <template v-slot:title>
            <bm-dropzone :accept="['folder']" :states="{ active: false }" :value="root">
                <mail-mailbox-icon :mailbox="mailbox" class="mr-1" />
                <span class="font-weight-bold text-left">{{ mailbox.name }}</span>
            </bm-dropzone>
        </template>
        <template v-slot:footer>
            <mail-folder-input v-if="mailbox.writable" :mailboxes="[mailbox]" @submit="name => add(name, mailbox)" />
        </template>
    </mail-folder-tree>
    <folder-list-loading v-else :name="mailbox.name" />
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { BmDropzone } from "@bluemind/styleguide";
import { MAILBOX_ROOT_FOLDERS } from "~/getters";
import { CREATE_FOLDER } from "~/actions";
import { createRoot } from "~/model/folder";
import MailFolderTree from "./MailFolderTree";
import FolderListLoading from "./FolderListLoading";
import MailFolderInput from "../MailFolderInput";
import { LoadingStatus } from "~/model/loading-status";
import MailMailboxIcon from "../MailMailboxIcon";

export default {
    name: "UserFolders",
    components: { MailMailboxIcon, MailFolderInput, MailFolderTree, FolderListLoading, BmDropzone },
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
        },
        root() {
            return createRoot(this.mailbox);
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
