<script setup>
import { computed } from "vue";
import store from "@bluemind/store";

import { CONVERSATION_LIST_COUNT } from "~/getters";

import FolderResultContentEmpty from "./FolderResultContentEmpty";
import MailConversationListWrapper from "./MailConversationListWrapper";

const count = computed(() => store.getters[`mail/${CONVERSATION_LIST_COUNT}`]);
</script>

<template>
    <folder-result-content-empty v-if="count === 0" />
    <mail-conversation-list-wrapper v-else-if="count > 0" class="flex-fill" />
</template>
