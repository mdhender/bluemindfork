<template>
    <mail-multiple-selection-actions v-if="SEVERAL_CONVERSATIONS_SELECTED" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import { mapGetters, mapMutations } from "vuex";
import { SEVERAL_CONVERSATIONS_SELECTED, SELECTION } from "~/getters";
import { RESET_ACTIVE_MESSAGE, SET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "MailActionsPanel",
    components: {
        MailMessageStarter,
        MailMultipleSelectionActions
    },
    computed: {
        ...mapGetters("mail", { SEVERAL_CONVERSATIONS_SELECTED, SELECTION })
    },
    watch: {
        ONE_CONVERSATION_SELECTED: {
            handler: function (value) {
                if (value) {
                    this.SET_ACTIVE_MESSAGE(this.SELECTION[0]);
                } else {
                    this.RESET_ACTIVE_MESSAGE();
                    this.UNSET_CURRENT_CONVERSATION();
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", { RESET_ACTIVE_MESSAGE, SET_ACTIVE_MESSAGE, UNSET_CURRENT_CONVERSATION })
    }
};
</script>
