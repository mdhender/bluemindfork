<template>
    <chain-of-responsibility :is-responsible="SEVERAL_CONVERSATIONS_SELECTED">
        <mail-multiple-selection-actions />
    </chain-of-responsibility>
</template>
<script>
import { mapGetters } from "vuex";
import { SEVERAL_CONVERSATIONS_SELECTED } from "~/getters";
import ChainOfResponsibility from "../ChainOfResponsibility";
export default {
    name: "MailMultipleSelectionScreen",
    components: { ChainOfResponsibility },
    computed: {
        ...mapGetters("mail", { SEVERAL_CONVERSATIONS_SELECTED })
    },

    priority: 1024
};
</script>
