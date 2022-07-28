<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.ERROR" :path="path" tag="span">
        <template #name>
            <router-link :to="folderRoute(folder)">
                <strong><mail-folder-icon :folder="folder" :mailbox="payload.mailbox" /></strong>
            </router-link>
        </template>
    </i18n>
    <default-alert v-else-if="alert.type === AlertTypes.SUCCESS" :alert="alert" :options="{ name }" />
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import { MailRoutesMixin } from "~/mixins";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "RemoveFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin, MailRoutesMixin],
    computed: {
        folder() {
            return this.payload.folder;
        },
        name() {
            return this.folder.name;
        }
    }
};
</script>
