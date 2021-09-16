<template>
    <div class="h-100">
        <folder-result-content-empty v-if="CONVERSATION_LIST_COUNT === 0" />
        <mail-conversation-list-wrapper v-if="CONVERSATION_LIST_COUNT > 0" class="h-100" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import FolderResultContentEmpty from "./FolderResultContentEmpty";
import MailConversationListWrapper from "./MailConversationListWrapper";
import { CONVERSATION_LIST_COUNT } from "~/getters";

export default {
    components: {
        FolderResultContentEmpty,
        MailConversationListWrapper
    },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_COUNT })
    }
};
</script>

<style></style>
