<template>
    <i18n :path="path" tag="span">
        <template #count>{{ payload.conversations.length }}</template>
        <template #subject>{{ payload.conversations[0].subject?.trim() || $t("mail.viewer.no.subject") }}</template>
        <template #folder>
            <folder-route-link :folder="payload.folder" />
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "MoveConversations",
    components: { FolderRouteLink },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["mailboxes"]),
        path() {
            const plurality = this.payload.conversations.length > 1 ? "plural" : "single";
            return `alert.${this.alert.name.toLowerCase()}.${this.alert.type}.${plurality}`;
        }
    }
};
</script>
