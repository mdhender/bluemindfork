<template>
    <div class="h-100 bg-extra-light">
        <search-result-content v-if="CONVERSATION_LIST_IS_RESOLVED" />
        <search-result-spinner v-if="CONVERSATION_LIST_IS_LOADING" />
        <search-result-error v-if="CONVERSATION_LIST_IS_REJECTED" />
    </div>
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

<style></style>
