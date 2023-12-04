<template>
    <search-result-content-empty v-if="CONVERSATION_LIST_COUNT === 0" :pattern="pattern" />
    <mail-conversation-list-wrapper v-else class="flex-fill" />
</template>

<script>
import { mapGetters, mapState } from "vuex";
import SearchResultContentEmpty from "./SearchResultContentEmpty";
import MailConversationListWrapper from "./MailConversationListWrapper";
import { CONVERSATION_LIST_COUNT } from "~/getters";

export default {
    name: "SearchResultContent",
    components: {
        SearchResultContentEmpty,
        MailConversationListWrapper
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT }),
        ...mapState("mail", { pattern: state => state.conversationList.search.searchQuery.pattern })
    }
};
</script>
