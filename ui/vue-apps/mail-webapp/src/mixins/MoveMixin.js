import { mapActions, mapGetters, mapState } from "vuex";

import { MOVE_CONVERSATIONS, MOVE_MESSAGES, CREATE_FOLDER_HIERARCHY } from "~/actions";
import { IS_CURRENT_CONVERSATION, MY_MAILBOX, NEXT_CONVERSATION } from "~/getters";

export default {
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

    moveAction(payload);
}

function navigateConversations(action) {
    return function ({ conversations, folder }) {
        conversations = Array.isArray(conversations) ? conversations : [conversations];
        action.call(this, { conversations, folder });
        if (conversations.length === 1 && this.$store.getters["mail/" + IS_CURRENT_CONVERSATION](conversations[0])) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION];
            this.$router.navigate({ name: "v:mail:conversation", params: { conversation: next } });
        }
    };
}

function navigateConversationMessage(action) {
    return function ({ conversation, message, folder }) {
        const messageInFolder = conversation.messages.filter(m => m.folderRef.key === conversation.folderRef.key);
        action.call(this, { conversation, messages: [message], folder });
        if (messageInFolder.length === 1 && messageInFolder[0].key === message.key) {
            const next = this.$store.getters["mail/" + NEXT_CONVERSATION];
            this.$router.navigate({ name: "v:mail:conversation", params: { conversation: next } });
        }
    };
}
