<template>
    <i18n :path="path" tag="span">
        <template #folder> <mail-folder-icon :folder="folder" :mailbox="payload.mailbox" /> </template>
        <template #destination>
            <folder-route-link v-if="destination" :folder="destination" />
            <span v-else> {{ $t("alert.mail.move_folder.to_mailbox_root", { mailbox: mailbox }) }}</span>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink.vue";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MoveFolder",
    components: { FolderRouteLink, MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
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
