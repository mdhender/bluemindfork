<template>
    <i18n :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :mailbox="mailboxes[folder.mailboxRef.key]" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "EmptyFolder",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        folder() {
            return this.folders[this.payload.folder.key];
        }
    }
};
</script>
