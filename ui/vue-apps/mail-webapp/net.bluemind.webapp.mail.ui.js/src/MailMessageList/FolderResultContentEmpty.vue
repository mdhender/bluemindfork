<template>
    <folder-result-content-empty-filter v-if="areMessagesFiltered" />
    <folder-result-content-empty-folder v-else />
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmptyFolder from "./FolderResultContentEmptyFolder";
import FolderResultContentEmptyFilter from "./FolderResultContentEmptyFilter";
export default {
    name: "FolderResultContentEmpty",
    components: {
        FolderResultContentEmptyFolder,
        FolderResultContentEmptyFilter
    },
    computed: {
        ...mapGetters("mail-webapp", ["areMessagesFiltered"])
    }
};
</script>

<style></style>
