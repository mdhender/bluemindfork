<template>
    <i18n :path="path" tag="span">
        <template #name>
            <router-link :to="link">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "../../model/mailbox";

export default {
    name: "EmptyFolder",
    components: { MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        link() {
            return { name: "v:mail:home", params: { folder: this.folder.path } };
        },
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.folders[this.payload.folder.key];
        }
    }
};
</script>
