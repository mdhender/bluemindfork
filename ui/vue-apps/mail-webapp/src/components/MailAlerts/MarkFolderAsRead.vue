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
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "../../model/mailbox";

export default {
    name: "MarkFolderAsRead",
    components: { MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
        link() {
            return { name: "v:mail:home", params: { folder: this.alert.payload.folder.path } };
        },
        shared() {
            return this.alert.payload.mailbox.type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.alert.payload.folder;
        }
    }
};
</script>
