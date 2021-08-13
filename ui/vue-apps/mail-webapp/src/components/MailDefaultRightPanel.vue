<template>
    <mail-conversation-panel v-if="ONE_CONVERSATION_SELECTED" />
    <mail-multiple-selection-actions v-else-if="SEVERAL_CONVERSATIONS_SELECTED" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import { mapGetters, mapMutations } from "vuex";
import {
    CONVERSATION_MESSAGE_BY_KEY,
    MY_SENT,
    ONE_CONVERSATION_SELECTED,
    SEVERAL_CONVERSATIONS_SELECTED,
    SELECTION
} from "~/getters";
import {
    RESET_ACTIVE_MESSAGE,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import MailConversationPanel from "./MailThread/MailConversationPanel";

export default {
    name: "MailDefaultRightPanel",
    components: {
        MailConversationPanel,
        MailMessageStarter,
        MailMultipleSelectionActions
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            MY_SENT,
            ONE_CONVERSATION_SELECTED,
            SEVERAL_CONVERSATIONS_SELECTED,
            SELECTION
        })
    },
    watch: {
        SELECTION: {
            handler: function () {
                if (this.ONE_CONVERSATION_SELECTED) {
                    this.SET_ACTIVE_MESSAGE({ key: this.SELECTION[0].messages[0] });
                    this.SET_CURRENT_CONVERSATION(this.SELECTION[0]);
                } else {
                    this.RESET_ACTIVE_MESSAGE();
                    this.UNSET_CURRENT_CONVERSATION();
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            SET_ACTIVE_MESSAGE,
            SET_CURRENT_CONVERSATION,
            UNSET_CURRENT_CONVERSATION
        })
    }
};
</script>
