<template>
    <chain-of-responsibility :is-responsible="ONE_CONVERSATION_SELECTED">
        <mail-conversation-panel />
    </chain-of-responsibility>
</template>
<script>
import { mapGetters } from "vuex";
import { ONE_CONVERSATION_SELECTED } from "~/getters";
import ChainOfResponsibility from "../ChainOfResponsibility";
export default {
    name: "MailOneSelectionScreen",
    components: { ChainOfResponsibility },
    computed: {
        ...mapGetters("mail", { ONE_CONVERSATION_SELECTED })
    },

    priority: 1025
};
</script>
