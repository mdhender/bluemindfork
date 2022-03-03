<template>
    <i18n :path="path" tag="span">
        <template #folder> <mail-folder-icon :folder="folder" :shared="shared" /> </template>
        <template #destination>
            <router-link v-if="destination" :to="folderRoute(destination)">
                <strong><mail-folder-icon :folder="destination" :shared="shared" /></strong>
            </router-link>
            <span v-else> {{ $t("alert.mail.move_folder.to_mailbox_root", { mailbox }) }}</span>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MoveFolder",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        shared() {
            return this.payload.mailbox.type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.payload.folder;
        },
        destination() {
            return this.payload.parent;
        },
        mailbox() {
            return this.$store.state.mail.mailboxes[this.folder.mailboxRef.key].name;
        }
    }
};
</script>
