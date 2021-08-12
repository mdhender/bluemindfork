<template>
    <mail-message v-if="ONE_CONVERSATION_SELECTED && firstSelectedConversationHasOnlyOneMessage" />
    <mail-thread v-else-if="ONE_CONVERSATION_SELECTED" />
    <mail-multiple-selection-actions v-else-if="SEVERAL_CONVERSATIONS_SELECTED" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import { mapGetters } from "vuex";
import { ONE_CONVERSATION_SELECTED, SEVERAL_CONVERSATIONS_SELECTED, SELECTION } from "~/getters";
import MailMessage from "./MailThread/MailMessage";
import MailThread from "./MailThread/MailThread";

export default {
    name: "MailActionsPanel",
    components: {
        MailMessage,
        MailMessageStarter,
        MailMultipleSelectionActions,
        MailThread
    },
    computed: {
        ...mapGetters("mail", { ONE_CONVERSATION_SELECTED, SEVERAL_CONVERSATIONS_SELECTED, SELECTION }),
        firstSelectedConversationHasOnlyOneMessage() {
            return this.SELECTION[0]?.messages.length === 1;
        }
    }
};
</script>
