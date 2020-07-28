<template>
    <div class="h-100 bg-extra-light">
        <search-result-content v-if="isResolved" />
        <search-result-spinner v-if="isLoading" />
        <search-result-error v-if="isRejected" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import SearchResultSpinner from "./SearchResultSpinner";
import SearchResultError from "./SearchResultError";
import SearchResultContent from "./SearchResultContent";

export default {
    name: "SearchResult",
    components: {
        SearchResultSpinner,
        SearchResultError,
        SearchResultContent
    },
    computed: {
        ...mapGetters("mail-webapp/search", ["isLoading", "isRejected", "isResolved"])
    }
};
</script>

<style></style>
