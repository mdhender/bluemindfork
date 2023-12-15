<template>
    <i18n :path="path" tag="span">
        <template #name>
            <folder-route-link :folder="folder" />
        </template>
    </i18n>
</template>
<script>
import { AlertMixin } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "MarkFolderAsRead",
    components: { FolderRouteLink },
    mixins: [AlertMixin],
    computed: {
        folder() {
            return this.payload.folder;
        }
    }
};
</script>
