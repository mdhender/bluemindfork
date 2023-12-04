<script setup>
import { computed } from "vue";
import store from "@bluemind/store";

import { CONVERSATION_LIST_IS_LOADING, CONVERSATION_LIST_IS_RESOLVED } from "~/getters";

import FolderResultContent from "./FolderResultContent";
import FolderResultSpinner from "./FolderResultSpinner";

const isLoading = computed(() => store.gettters[`mail/${CONVERSATION_LIST_IS_LOADING}`]);
const isResolved = computed(() => store.gettters[`mail/${CONVERSATION_LIST_IS_RESOLVED}`]);
</script>

<template>
    <folder-result-content v-if="CONVERSATION_LIST_IS_RESOLVED" />
    <folder-result-spinner v-else-if="CONVERSATION_LIST_IS_LOADING" />
</template>
