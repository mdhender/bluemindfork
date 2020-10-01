<template>
    <div class="h-100 d-flex flex-column">
        <search-result-content-header />
        <search-result-content-empty v-if="MESSAGE_LIST_COUNT === 0" class="flex-fill" :pattern="search.pattern" />
        <message-list v-if="MESSAGE_LIST_COUNT > 0" class="flex-fill" />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import SearchResultContentHeader from "./SearchResultContentHeader";
import SearchResultContentEmpty from "./SearchResultContentEmpty";
import MessageList from "./MessageList";

export default {
    name: "SearchResultContent",
    components: {
        SearchResultContentHeader,
        SearchResultContentEmpty,
        MessageList
    },
    computed: {
        ...mapGetters("mail", ["MESSAGE_LIST_COUNT"]),
        ...mapState("mail-webapp", ["search"])
    }
};
</script>
