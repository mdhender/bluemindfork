<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.ERROR" :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
    <default-alert v-else-if="alert.type === AlertTypes.SUCCESS" :alert="alert" :options="{ name }" />
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { mailboxUtils } from "@bluemind/mail";
import { MailRoutesMixin } from "~/mixins";
import MailFolderIcon from "../MailFolderIcon";

const { MailboxType } = mailboxUtils;

export default {
    name: "RemoveFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        shared() {
            return this.payload.mailbox.type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.payload.folder;
        },
        name() {
            return this.folder.name;
        }
    }
};
</script>
