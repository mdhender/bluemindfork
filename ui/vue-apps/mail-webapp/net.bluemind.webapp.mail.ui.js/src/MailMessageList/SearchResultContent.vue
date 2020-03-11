<template>
    <div>
        <search-result-content-header />
        <search-result-content-empty v-if="count === 0" :pattern="search.pattern" />
        <message-list v-if="count > 0" class="h-100" />
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
        ...mapGetters("mail-webapp/messages", ["count"]),
        ...mapState("mail-webapp", ["search"])
    }
};
</script>

<style lang="scss"></style>
