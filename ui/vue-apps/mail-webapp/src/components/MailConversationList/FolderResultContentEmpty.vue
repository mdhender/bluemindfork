<template>
    <folder-result-content-empty-filter v-if="CONVERSATION_LIST_FILTERED" />
    <folder-result-content-empty-folder v-else />
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmptyFolder from "./FolderResultContentEmptyFolder";
import FolderResultContentEmptyFilter from "./FolderResultContentEmptyFilter";
import { CONVERSATION_LIST_FILTERED } from "~/getters";

export default {
    name: "FolderResultContentEmpty",
    components: {
        FolderResultContentEmptyFolder,
        FolderResultContentEmptyFilter
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_FILTERED })
    }
};
</script>
