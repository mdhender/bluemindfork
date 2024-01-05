<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { MAILBOX_TRASH } from "~/getters";
import FolderRouteLink from "../FolderRouteLink";

const props = defineProps({
    alert: {
        type: Object,
        required: true
    }
});
const messages = computed(() => props.alert.payload.conversations);
const isSingleMessage = computed(() => messages.value.length === 1);
const mailbox = computed(() => {
    const { folders, mailboxes } = store.state.mail;
    const mailboxKey = folders[messages.value[0].folderRef.key].mailboxRef.key;
    return mailboxes[mailboxKey];
});
const trash = computed(() => store.getters[`mail/${MAILBOX_TRASH}`](mailbox.value));
</script>

<template>
    <div class="mark-messages-as-not-deleted-alert">
        <div>
            <i18n :path="`alert.mail.unexpunge.${alert.type}.${isSingleMessage ? 'single' : 'plural'}`">
                <template v-if="isSingleMessage" #subject>{{ messages[0].subject }}</template>
                <template v-else #count> {{ messages.length }} </template>
                <template #trash><folder-route-link :folder="trash" /></template>
            </i18n>
        </div>
    </div>
</template>
