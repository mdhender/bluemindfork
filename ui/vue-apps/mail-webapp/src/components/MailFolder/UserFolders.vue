<template>
    <mail-folder-tree
        v-if="isLoaded"
        :tree="MAILBOX_ROOT_FOLDERS(mailbox)"
        :collapsed="collapsed"
        @toggle-tree="toggleTree"
    >
        <template #title>
            <bm-dropzone :accept="['folder']" :states="{ active: false }" :value="root">
                <folder-tree-header :mailbox="mailbox" />
            </bm-dropzone>
        </template>
        <template #footer>
            <mail-folder-input
                v-if="mailbox.writable"
                size="sm"
                :mailboxes="[mailbox]"
                @submit="name => add(name, mailbox)"
            />
        </template>
    </mail-folder-tree>
    <folder-list-loading v-else :name="mailbox.name" />
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { folderUtils, loadingStatusUtils } from "@bluemind/mail";
import { BmDropzone } from "@bluemind/ui-components";
import { CREATE_FOLDER } from "~/actions";
import { MAILBOX_ROOT_FOLDERS } from "~/getters";
import { FolderTreeMixin } from "~/mixins";
import MailFolderTree from "./MailFolderTree";
import FolderListLoading from "./FolderListLoading";
import MailFolderInput from "../MailFolderInput";
import FolderTreeHeader from "./FolderTreeHeader";

const { createRoot } = folderUtils;
const { LoadingStatus } = loadingStatusUtils;

export default {
    name: "UserFolders",
    components: { MailFolderInput, MailFolderTree, FolderListLoading, BmDropzone, FolderTreeHeader },
    mixins: [FolderTreeMixin],
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
        },
        treeKey() {
            return this.mailbox.key;
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
