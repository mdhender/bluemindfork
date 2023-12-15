<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #name>
            <folder-route-link :folder="folder" />
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "RenameFolder",
    components: { DefaultAlert, FolderRouteLink },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        name() {
            return this.payload.name;
        },
        folder() {
            return this.folders[this.payload.folder.key];
        }
    }
};
</script>
