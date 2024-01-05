<template>
    <div v-if="CONVERSATION_LIST_DELETED_FILTER_ENABLED" class="folder-result-content-empty-deleted">
        {{ $tc("mail.list.recoverable.empty") }}
    </div>
    <folder-result-content-empty-filter v-else-if="CONVERSATION_LIST_FILTERED" />
    <folder-result-content-empty-folder v-else />
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmptyFolder from "./FolderResultContentEmptyFolder";
import FolderResultContentEmptyFilter from "./FolderResultContentEmptyFilter";
import { CONVERSATION_LIST_FILTERED, CONVERSATION_LIST_DELETED_FILTER_ENABLED } from "~/getters";

export default {
    name: "FolderResultContentEmpty",
    components: {
        FolderResultContentEmptyFolder,
        FolderResultContentEmptyFilter
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_FILTERED, CONVERSATION_LIST_DELETED_FILTER_ENABLED })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";

.folder-result-content-empty-deleted {
    display: flex;
    flex-direction: column;
    padding: $sp-8 $sp-5;
    align-items: center;
    text-align: center;
}
</style>
