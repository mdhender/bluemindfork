<template>
    <i18n :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import { mailbox } from "@bluemind/mail";
import MailFolderIcon from "../MailFolderIcon";
import { MailRoutesMixin } from "~/mixins";

const { MailboxType } = mailbox;

export default {
    name: "MarkFolderAsRead",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        shared() {
            return this.payload.mailbox.type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.payload.folder;
        }
    }
};
</script>
