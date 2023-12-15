<script>
import { mapState } from "vuex";
import store from "@bluemind/store";
import { MailRoutesMixin } from "~/mixins";
import MailFolderIcon from "./MailFolderIcon";

export default {
    components: { MailFolderIcon },
    mixins: [MailRoutesMixin],
    props: {
        folder: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["mailboxes"])
    }
};
</script>

<template>
    <router-link :to="folderRoute(folder)">
        <strong><mail-folder-icon :folder="folder" :mailbox="mailboxes[folder.mailboxRef.key]" /></strong>
    </router-link>
</template>
