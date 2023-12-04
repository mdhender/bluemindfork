<script setup>
import { computed } from "vue";
import store from "@bluemind/store";

import { CURRENT_MAILBOX, MAILBOX_TRASH } from "~/getters";

import TrashResultHeader from "./TrashResultHeader";
import MailConversationListHeader from "./MailConversationListHeader";

const { getters, state } = store;
const isTrash = computed(
    () =>
        getters[`mail/${CURRENT_MAILBOX}`] &&
        getters[`mail/${MAILBOX_TRASH}`](getters[`mail/${CURRENT_MAILBOX}`])?.key === state.mail.activeFolder
);
</script>

<template>
    <mail-conversation-list-header>
        <trash-result-header v-if="isTrash" />
    </mail-conversation-list-header>
</template>
