import { mapActions, mapGetters, mapState } from "vuex";

import { MOVE_MESSAGES, CREATE_FOLDER_HIERARCHY } from "~actions";
import { IS_ACTIVE_MESSAGE, MY_MAILBOX } from "~getters";
import { equal } from "../model/message";

export default {
    computed: {
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
        let next = this.$store.state.mail.messages[this.$store.getters["mail-webapp/nextMessageKey"]];
        messages = Array.isArray(messages) ? messages : [messages];
        action.call(this, { messages, folder });
        if (messages.length === 1 && this.$store.getters["mail/" + IS_ACTIVE_MESSAGE](messages[0])) {
            //TODO: This is a hack because nextMessageKey from deprecated store can return a wrong nex key
            if (equal(next, messages[0])) {
                next = null;
            }
            this.$router.navigate({ name: "v:mail:message", params: { message: next } });
        }
    };
}
