<template>
    <mail-conversation-panel v-if="ONE_CONVERSATION_SELECTED" />
    <mail-multiple-selection-actions v-else-if="SEVERAL_CONVERSATIONS_SELECTED" />
    <mail-home-screen v-else />
</template>
<script>
import { mapGetters, mapMutations } from "vuex";

import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import {
    CONVERSATION_METADATA,
    MY_MAILBOX,
    ONE_CONVERSATION_SELECTED,
    SEVERAL_CONVERSATIONS_SELECTED,
    SELECTION_KEYS
} from "~/getters";
import {
    RESET_ACTIVE_MESSAGE,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import MailConversationPanel from "./MailThread/MailConversationPanel";
import MailHomeScreen from "./MailHomeScreen";

export default {
    name: "MailRouteHome",
    components: {
        MailConversationPanel,
        MailHomeScreen,
        MailMultipleSelectionActions
    },
    data() {
        return { mailboxLoaded: false };
    },
    computed: {
        ...mapGetters("mail", {
            CONVERSATION_METADATA,
            MY_MAILBOX,
            ONE_CONVERSATION_SELECTED,
            SEVERAL_CONVERSATIONS_SELECTED,
            SELECTION_KEYS
        })
    },
    watch: {
        SELECTION_KEYS: {
            handler: function () {
                if (this.ONE_CONVERSATION_SELECTED) {
                    const conversation = this.CONVERSATION_METADATA(this.SELECTION_KEYS[0]);
                    this.SET_ACTIVE_MESSAGE({ key: conversation.messages[0] });
                    this.SET_CURRENT_CONVERSATION(conversation);
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
