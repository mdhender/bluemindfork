<template>
    <i18n :path="path" tag="span">
        <template #name>
            <folder-route-link :folder="folder" />
        </template>
    </i18n>
</template>
<script>
import { mapState } from "vuex";
import { AlertMixin } from "@bluemind/alert.store";
import FolderRouteLink from "../FolderRouteLink";

export default {
    name: "EmptyFolder",
    components: { FolderRouteLink },
    mixins: [AlertMixin],
    computed: {
        ...mapState("mail", ["folders", "mailboxes"]),
        folder() {
            return this.folders[this.payload.folder.key];
        }
    }
};
</script>
