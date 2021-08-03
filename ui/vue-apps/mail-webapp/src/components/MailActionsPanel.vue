<template>
    <mail-message v-if="ONE_CONVERSATION_SELECTED && firstSelectedConversationHasOnlyOneMessage" />
    <mail-thread v-else-if="ONE_CONVERSATION_SELECTED" />
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
import { removeSentDuplicates } from "~/model/conversations";
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
        ...mapGetters("mail", {
            CONVERSATION_MESSAGE_BY_KEY,
            MY_SENT,
            ONE_CONVERSATION_SELECTED,
            SEVERAL_CONVERSATIONS_SELECTED,
            SELECTION
        }),
        firstSelectedConversationHasOnlyOneMessage() {
            const firstConversation = this.SELECTION[0];
            const messages = removeSentDuplicates(
                this.CONVERSATION_MESSAGE_BY_KEY(firstConversation.key),
                this.MY_SENT
            );
            return messages.length === 1;
        }
    },
    watch: {
        SELECTION: {
            handler: function () {
                if (this.ONE_CONVERSATION_SELECTED) {
                    if (this.firstSelectedConversationHasOnlyOneMessage) {
                        this.SET_ACTIVE_MESSAGE(this.SELECTION[0].messages[0]);
                    } else {
                        this.SET_CURRENT_CONVERSATION(this.SELECTION[0]);
                    }
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
