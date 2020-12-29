import { mapActions, mapGetters, mapState } from "vuex";

import { MOVE_MESSAGES, CREATE_FOLDER_HIERARCHY } from "~actions";
import { MY_MAILBOX } from "~getters";

export default {
    computed: {
        ...mapGetters("mail-webapp", { $_MoveMixin_next: "nextMessageKey" }),
        ...mapState("mail-webapp/currentMessage", { $_MoveMixin_current: "key" }),
        ...mapState("mail", {
            $_MoveMixin_mailboxes: "mailboxes",
            $_MoveMixin_folders: "folders"
        }),
        ...mapGetters("mail", { $_MoveMixin_mailbox: MY_MAILBOX })
    },
    methods: {
        ...mapActions("mail", { $_MoveMixin_create: CREATE_FOLDER_HIERARCHY, $_MoveMixin_move: MOVE_MESSAGES }),
        MOVE_MESSAGES: navigate(async function ({ messages, folder }) {
            if (folder.key) {
                folder = this.$_MoveMixin_folders[folder.key] || folder;
            }
            move(messages, folder, this.$_MoveMixin_mailbox, this.$_MoveMixin_create, this.$_MoveMixin_move);
        })
    }
};

async function move(messages, folder, mailbox, createAction, moveAction) {
    if (!folder.key) {
        folder = await createAction({ ...folder, mailbox });
    }

    moveAction({ messages, folder });
}

function navigate(action) {
    return function ({ messages, folder }) {
        const next = this.$_MoveMixin_next;
        messages = Array.isArray(messages) ? messages : [messages];
        action.call(this, { messages, folder });
        if (messages.length === 1 && messages[0].key === this.$_MoveMixin_current) {
            this.$router.navigate({ name: "v:mail:message", params: { message: next } });
        }
    };
}
