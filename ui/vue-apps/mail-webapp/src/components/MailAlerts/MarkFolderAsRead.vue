<template>
    <i18n :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :mailbox="payload.mailbox" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MarkFolderAsRead",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        folder() {
            return this.payload.folder;
        }
    }
};
</script>
