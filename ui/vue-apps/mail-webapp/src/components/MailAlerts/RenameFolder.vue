<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name, previousName }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ previousName }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #previousName>{{ previousName }}</template>
        <template #name>
            <router-link :to="link">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~model/mailbox";

export default {
    name: "RenameFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        link() {
            return { name: "v:mail:home", params: { folder: this.folder.path } };
        },
        name() {
            return this.payload.name;
        },
        shared() {
            return this.mailboxes[this.folder.mailboxRef.key].type === MailboxType.MAILSHARE;
        },
        previousName() {
            return this.payload.folder.name;
        },
        folder() {
            return this.folders[this.payload.folder.key];
        }
    }
};
</script>
