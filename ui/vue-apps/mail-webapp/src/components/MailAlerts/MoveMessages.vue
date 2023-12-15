<template>
    <i18n :path="path" tag="span">
        <template #count>{{ payload.messages.length }}</template>
        <template #subject>{{ payload.messages[0].subject.trim() || $t("mail.viewer.no.subject") }}</template>
        <template #folder>
            <folder-route-link v-if="destination" :folder="payload.folder" />
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "MoveMessages",
    components: { FolderRouteLink },
    mixins: [AlertMixin],
    computed: {
        path() {
            const plurality = this.payload.messages.length > 1 ? "plural" : "single";
            return `alert.mail.move_messages.${this.alert.type}.${plurality}`;
        }
    }
};
</script>
