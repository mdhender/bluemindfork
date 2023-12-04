<script setup>
import { computed } from "vue";
import store from "@bluemind/store";

import { CONVERSATION_LIST_IS_RESOLVED } from "~/getters";

import SearchResultContentHeader from "./SearchResultContentHeader";
import MailConversationListHeader from "./MailConversationListHeader";

const { getters, state } = store;
const isResolved = computed(() => getters[`mail/${CONVERSATION_LIST_IS_RESOLVED}`]);
</script>

<template>
    <mail-conversation-list-header>
        <search-result-content-header v-if="isResolved" />
    </mail-conversation-list-header>
</template>
