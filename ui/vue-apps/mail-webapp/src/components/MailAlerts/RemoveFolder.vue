<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.ERROR" :path="path" tag="span">
        <template #name>
            <folder-route-link :folder="folder" />
        </template>
    </i18n>
    <default-alert v-else-if="alert.type === AlertTypes.SUCCESS" :alert="alert" :options="{ name }" />
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "RemoveFolder",
    components: { DefaultAlert, FolderRouteLink },
    mixins: [AlertMixin],
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
