<template>
    <folder-result-content-empty-filter v-if="MESSAGE_LIST_FILTERED" />
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
        ...mapGetters("mail", ["MESSAGE_LIST_FILTERED"])
    }
};
</script>
