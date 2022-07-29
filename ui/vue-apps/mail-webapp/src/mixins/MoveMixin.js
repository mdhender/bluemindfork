import { mapActions, mapGetters, mapState } from "vuex";
import { loadingStatusUtils } from "@bluemind/mail";

import { MOVE_CONVERSATIONS, MOVE_MESSAGES, CREATE_FOLDER_HIERARCHY } from "~/actions";
import { CONVERSATIONS_ACTIVATED, CURRENT_MAILBOX, IS_CURRENT_CONVERSATION, NEXT_CONVERSATION } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

const { LoadingStatus } = loadingStatusUtils;

export default {
    mixins: [MailRoutesMixin],
    computed: {
        ...mapState("mail", {
            $_MoveMixin_mailboxes: "mailboxes",
            $_MoveMixin_folders: "folders"
        }),
        ...mapGetters("mail", { $_MoveMixin_mailbox: CURRENT_MAILBOX })
    },
    methods: {
        ...mapActions("mail", {
            $_MoveMixin_create: CREATE_FOLDER_HIERARCHY,
            $_MoveMixin_moveConversations: MOVE_CONVERSATIONS,
            $_MoveMixin_move: MOVE_MESSAGES
        }),
        MOVE_CONVERSATION_MESSAGE: navigateConversationMessage(moveMessages),
        MOVE_CONVERSATIONS: navigateConversations(moveConversations)
    }
};

async function moveMessages({ conversation, messages, folder }) {
    if (folder.key) {
        folder = this.$_MoveMixin_folders[folder.key] || folder;
    }
    await move(
        { conversation, messages, folder },
        this.$_MoveMixin_mailbox,
        this.$_MoveMixin_create,
        this.$_MoveMixin_move
    );
}

async function moveConversations({ conversations, folder }) {
    if (folder.key) {
        folder = this.$_MoveMixin_folders[folder.key] || folder;
    }
    return move(
        {
            conversations,
            mailbox: this.$_MoveMixin_mailbox,
            folder,
            conversationsActivated: this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`]
        },
        this.$_MoveMixin_mailbox,
        this.$_MoveMixin_create,
        this.$_MoveMixin_moveConversations
    );
}

async function move(payload, mailbox, createAction, moveAction) {
    let { folder } = payload;
    if (!folder.key) {
        folder = await createAction({ ...folder, mailbox });
    }
    moveAction({ ...payload, folder });
}

function navigateConversations(action) {
    return function ({ conversations, folder }) {
        conversations = Array.isArray(conversations) ? conversations : [conversations];
        const next = this.$store.getters["mail/" + NEXT_CONVERSATION](conversations);
        const isCurrentConversation = this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversations[0]);
        const promise = action.call(this, { conversations, folder });
        if (isCurrentConversation) {
            this.navigateTo(next, conversations[0].folderRef);
        }
        return promise;
    };
}

function navigateConversationMessage(action) {
    return function ({ conversation, message, folder }) {
        if (conversation) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION]([conversation]);
            const isCurrentConversation = this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversation);
            const messages = this.$store.state.mail.conversations.messages;
            const messageKeysInFolder = conversation.messages.filter(messageKey => {
                const message = messages[messageKey];
                return message.loading !== LoadingStatus.ERROR && message.folderRef.key === conversation.folderRef.key;
            });
            action.call(this, { conversation, messages: [message], folder });
            if (messageKeysInFolder.length === 1 && messageKeysInFolder[0] === message.key && isCurrentConversation) {
                this.navigateTo(next, conversation.folderRef);
            }
        } else {
            action.call(this, { messages: [message], folder });
            this.$router.push({ name: "mail:home" });
        }
    };
}
