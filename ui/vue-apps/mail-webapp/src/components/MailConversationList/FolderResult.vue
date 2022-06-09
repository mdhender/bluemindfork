<template>
    <div class="folder-result h-100">
        <folder-result-content v-if="CONVERSATION_LIST_IS_RESOLVED" />
        <folder-result-spinner v-if="CONVERSATION_LIST_IS_LOADING" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContent from "./FolderResultContent";
import FolderResultSpinner from "./FolderResultSpinner";
import { CONVERSATION_LIST_IS_LOADING, CONVERSATION_LIST_IS_RESOLVED } from "~/getters";

export default {
    name: "FolderResult",
    components: {
        FolderResultContent,
        FolderResultSpinner
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_IS_LOADING, CONVERSATION_LIST_IS_RESOLVED })
    }
};
</script>
