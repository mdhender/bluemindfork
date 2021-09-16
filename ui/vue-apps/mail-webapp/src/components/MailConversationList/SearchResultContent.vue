<template>
    <div class="h-100 d-flex flex-column">
        <search-result-content-header />
        <search-result-content-empty v-if="CONVERSATION_LIST_COUNT === 0" class="flex-fill" :pattern="search.pattern" />
        <mail-conversation-list-wrapper v-else class="flex-fill" />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import SearchResultContentHeader from "./SearchResultContentHeader";
import SearchResultContentEmpty from "./SearchResultContentEmpty";
import MailConversationListWrapper from "./MailConversationListWrapper";
import { CONVERSATION_LIST_COUNT } from "~/getters";

export default {
    name: "SearchResultContent",
    components: {
        SearchResultContentHeader,
        SearchResultContentEmpty,
        MailConversationListWrapper
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT }),
        ...mapState("mail", { search: state => state.conversationList.search })
    }
};
</script>
