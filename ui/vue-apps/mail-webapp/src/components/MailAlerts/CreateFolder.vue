<template>
    <default-alert v-if="alert.type === AlertTypes.LOADING" :alert="alert" :options="{ name }" />
    <default-alert v-else-if="alert.type === AlertTypes.ERROR" :alert="alert" :options="{ name }" />
    <i18n v-else-if="alert.type === AlertTypes.SUCCESS" :path="path" tag="span">
        <template #name>
            <folder-route-link :folder="folder" />
        </template>
    </i18n>
</template>
<script>
import { AlertMixin, DefaultAlert } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "CreateFolder",
    components: { DefaultAlert, FolderRouteLink },
    mixins: [AlertMixin],

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
