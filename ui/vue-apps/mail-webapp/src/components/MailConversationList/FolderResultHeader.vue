<script setup>
import { computed } from "vue";
import store from "@bluemind/store";

import { CONVERSATION_LIST_DELETED_FILTER_ENABLED, CURRENT_MAILBOX, MAILBOX_TRASH } from "~/getters";

import MailConversationListHeader from "./MailConversationListHeader";
import MailConversationListFilters from "./MailConversationListFilters";
import RecoverableResultHeader from "./RecoverableResultHeader";
import TrashResultHeader from "./TrashResultHeader";

const { getters, state } = store;
const mailbox = computed(() => getters[`mail/${CURRENT_MAILBOX}`]);
const folder = computed(() => state.mail.folders[state.mail.activeFolder]);
const isTrash = computed(
    () => folder.value && getters[`mail/${MAILBOX_TRASH}`](folder.value.mailboxRef)?.key === folder.value.key
);
const isRecoverable = computed(() => isTrash.value && getters[`mail/${CONVERSATION_LIST_DELETED_FILTER_ENABLED}`]);
</script>

<template>
    <mail-conversation-list-header>
        <template v-if="isRecoverable" #toolbar>
            <mail-conversation-list-filters disabled />
        </template>
        <recoverable-result-header v-if="isRecoverable" />
        <trash-result-header v-else-if="isTrash && folder.writable" />
    </mail-conversation-list-header>
</template>
