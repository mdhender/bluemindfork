<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "~/model/mailbox";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "RenameFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        name() {
            return this.payload.name;
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
