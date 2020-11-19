<template>
    <i18n :path="path" tag="span">
        <template #count>{{ message.count }}</template>
        <template #subject>{{ message.subject }}</template>
        <template #folder>
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
    name: "MoveMessages",
    components: { MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["mailboxes"]),
        link() {
            return { name: "v:mail:home", params: { folder: this.alert.payload.folder.path } };
        },
        shared() {
            return this.mailboxes[this.alert.payload.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.alert.payload.folder;
        },
        message() {
            if (Array.isArray(this.payload.messages)) {
                return this.payload.messages[0];
            } else {
                return this.payload.messages;
            }
        },
        count() {
            return Array.isArray(this.payload.messages) ? this.payload.messages.length : 1;
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
