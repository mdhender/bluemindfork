<template>
    <i18n :path="path" tag="span">
        <template #count>{{ count }}</template>
        <template #subject>{{ message.subject.trim() || $t("mail.viewer.no.subject") }}</template>
        <template #folder>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MoveMessages",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", ["mailboxes"]),
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.payload.folder || this.payload.trash;
        },
        message() {
            const messages = this.payload.messages || this.payload.conversations;
            return Array.isArray(messages) ? messages[0] : messages;
        },
        count() {
            const messages = this.payload.messages || this.payload.conversations;
            return Array.isArray(messages) ? messages.length : 1;
        },
        path() {
            const { name, type } = this.alert;
            return (
                "alert." + name.toLowerCase() + "." + type.toLowerCase() + "." + (this.count > 1 ? "plural" : "single")
            );
        }
    }
};
</script>
