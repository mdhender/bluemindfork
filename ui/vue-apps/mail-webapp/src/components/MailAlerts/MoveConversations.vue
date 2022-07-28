<template>
    <i18n :path="path" tag="span">
        <template #count>{{ payload.conversations.length }}</template>
        <template #subject>{{ payload.conversations[0].subject.trim() || $t("mail.viewer.no.subject") }}</template>
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
import { MailRoutesMixin } from "~/mixins";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MoveConversations",
    components: { MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        ...mapState("mail", ["mailboxes"]),
        path() {
            const plurality = this.payload.conversations.length > 1 ? "plural" : "single";
            return `alert.${this.alert.name.toLowerCase()}.${this.alert.type}.${plurality}`;
        }
    }
};
</script>
