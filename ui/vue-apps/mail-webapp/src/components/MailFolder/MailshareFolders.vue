<template>
    <folder-list-loading v-if="!isLoaded" />
    <mail-folder-tree v-else-if="MAILSHARES.length > 0" :tree="MAILSHARE_ROOT_FOLDERS" :name="$t('common.mailshares')">
        <template v-slot:avatar>
            <mail-mailbox-icon :mailbox="MAILSHARES[0]" class="mr-1" />
        </template>
    </mail-folder-tree>
</template>
<script>
import { mapGetters } from "vuex";
import MailFolderTree from "./MailFolderTree";
import FolderListLoading from "./FolderListLoading";
import { MAILBOXES_ARE_LOADED, MAILSHARE_ROOT_FOLDERS, MAILSHARES } from "~/getters";
import MailMailboxIcon from "../MailMailboxIcon";

export default {
    name: "MailshareFolders",
    components: { MailMailboxIcon, MailFolderTree, FolderListLoading },
    computed: {
        ...mapGetters("mail", { MAILSHARE_ROOT_FOLDERS, MAILSHARES, MAILBOXES_ARE_LOADED }),
        isLoaded() {
            return this.MAILBOXES_ARE_LOADED && (!this.MAILSHARES.length || this.MAILSHARE_ROOT_FOLDERS.length);
        }
    }
};
</script>
