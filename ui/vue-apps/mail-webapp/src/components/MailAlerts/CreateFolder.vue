<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :mailbox="payload.mailbox" /></strong>
            </router-link>
        </template>
    </i18n>
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { MailRoutesMixin } from "~/mixins";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "CreateFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        folder() {
            return this.result;
        },
        name() {
            return this.payload.name;
        }
    }
};
</script>
