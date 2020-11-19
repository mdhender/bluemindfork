<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.ERROR" :path="path" tag="span">
        <template #name>
            <router-link :to="link">
                <strong><mail-folder-icon :folder="folder" :shared="shared" /></strong>
            </router-link>
        </template>
    </i18n>
    <default-alert v-else-if="alert.type === AlertTypes.SUCCESS" :alert="alert" :options="{ name }" />
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import MailFolderIcon from "../MailFolderIcon";
import { MailboxType } from "../../model/mailbox";

export default {
    name: "RemoveFolder",
    components: { DefaultAlert, MailFolderIcon },
    mixins: [AlertMixin],
    computed: {
        link() {
            return { name: "v:mail:home", params: { folder: this.alert.result.path } };
        },
        shared() {
            return this.alert.payload.mailbox.type === MailboxType.MAILSHARE;
        },
        folder() {
            return this.alert.payload.folder;
        },
        name() {
            return this.alert.payload.folder.name;
        }
    }
};
</script>
