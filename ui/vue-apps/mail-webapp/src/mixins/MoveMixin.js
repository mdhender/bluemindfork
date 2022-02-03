import { mapActions, mapGetters, mapState } from "vuex";

import { MOVE_CONVERSATIONS, MOVE_MESSAGES, CREATE_FOLDER_HIERARCHY } from "~/actions";
import { IS_CURRENT_CONVERSATION, MY_MAILBOX, NEXT_CONVERSATION } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
import { LoadingStatus } from "../model/loading-status";

export default {
    mixins: [MailRoutesMixin],
    computed: {
        ...mapState("mail", {
            $_MoveMixin_mailboxes: "mailboxes",
            $_MoveMixin_folders: "folders"
        }),
        ...mapGetters("mail", { $_MoveMixin_mailbox: MY_MAILBOX })
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
    await move(
        { conversations, folder },
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
        action.call(this, { conversations, folder });
        if (isCurrentConversation) {
            this.navigateTo(next, conversations[0].folderRef);
        }
    };
}

function navigateConversationMessage(action) {
    return function ({ conversation, message, folder }) {
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
    };
}
