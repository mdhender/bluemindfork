<template>
    <div class="h-100 bg-extra-light">
        <folder-result-content v-if="MESSAGE_LIST_IS_RESOLVED" />
        <folder-result-spinner v-if="MESSAGE_LIST_IS_LOADING" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContent from "./FolderResultContent";
import FolderResultSpinner from "./FolderResultSpinner";

export default {
    name: "FolderResult",
    components: {
        FolderResultContent,
        FolderResultSpinner
    },
    computed: {
        ...mapGetters("mail", ["MESSAGE_LIST_IS_LOADING", "MESSAGE_LIST_IS_RESOLVED"])
    }
};
</script>
