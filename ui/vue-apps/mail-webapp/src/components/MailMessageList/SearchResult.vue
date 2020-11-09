<template>
    <div class="h-100 bg-extra-light">
        <search-result-content v-if="MESSAGE_LIST_IS_RESOLVED" />
        <search-result-spinner v-if="MESSAGE_LIST_IS_LOADING" />
        <search-result-error v-if="MESSAGE_LIST_IS_REJECTED" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import SearchResultSpinner from "./SearchResultSpinner";
import SearchResultError from "./SearchResultError";
import SearchResultContent from "./SearchResultContent";
import { MESSAGE_LIST_IS_LOADING, MESSAGE_LIST_IS_RESOLVED, MESSAGE_LIST_IS_REJECTED } from "~getters";

export default {
    name: "SearchResult",
    components: {
        SearchResultSpinner,
        SearchResultError,
        SearchResultContent
    },
    computed: {
        ...mapGetters("mail", { MESSAGE_LIST_IS_LOADING, MESSAGE_LIST_IS_RESOLVED, MESSAGE_LIST_IS_REJECTED })
    }
};
</script>

<style></style>
