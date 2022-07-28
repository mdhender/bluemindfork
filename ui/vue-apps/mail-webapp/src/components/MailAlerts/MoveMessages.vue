<template>
    <i18n :path="path" tag="span">
        <template #count>{{ payload.messages.length }}</template>
        <template #subject>{{ payload.messages[0].subject.trim() || $t("mail.viewer.no.subject") }}</template>
        <template #folder>
            <router-link :to="folderRoute(payload.folder)">
                <strong
                    ><mail-folder-icon :folder="payload.folder" :mailbox="mailboxes[payload.folder.mailboxRef.key]"
                /></strong>
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
    name: "MoveMessages",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", ["mailboxes"]),
        path() {
            const plurality = this.payload.messages.length > 1 ? "plural" : "single";
            return `alert.mail.move_messages.${this.alert.type}.${plurality}`;
        }
    }
};
</script>
