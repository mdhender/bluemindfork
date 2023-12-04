<template>
    <search-result-content v-if="CONVERSATION_LIST_IS_RESOLVED" />
    <search-result-spinner v-else-if="CONVERSATION_LIST_IS_LOADING" />
    <search-result-error v-else-if="CONVERSATION_LIST_IS_REJECTED" />
</template>

<script>
import { mapGetters } from "vuex";
import SearchResultSpinner from "./SearchResultSpinner";
import SearchResultError from "./SearchResultError";
import SearchResultContent from "./SearchResultContent";
import { CONVERSATION_LIST_IS_LOADING, CONVERSATION_LIST_IS_RESOLVED, CONVERSATION_LIST_IS_REJECTED } from "~/getters";

export default {
    name: "SearchResult",
    components: {
        SearchResultSpinner,
        SearchResultError,
        SearchResultContent
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_LIST_IS_LOADING,
            CONVERSATION_LIST_IS_RESOLVED,
            CONVERSATION_LIST_IS_REJECTED
        })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.search-result {
    background-color: $backdrop;
}
</style>
