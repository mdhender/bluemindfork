import { SET_MAILBOX_FOLDERS, TOGGLE_EDIT_FOLDER } from "~mutations";
import { MailboxType } from "../model/mailbox";

export default {
    state: {
        editing: undefined,
        myMailboxIsLoaded: false,
        mailsharesAreLoaded: false
    },
    mutations: {
        [TOGGLE_EDIT_FOLDER]: (state, key) => {
            if (state.editing && state.editing === key) {
                state.editing = undefined;
            } else {
                state.editing = key;
            }
        },
        // Hooks
        [SET_MAILBOX_FOLDERS]: (state, { mailbox }) => {
            if (mailbox.type === MailboxType.USER) {
                state.myMailboxIsLoaded = true;
            } else {
                state.mailsharesAreLoaded = true;
            }
        }
    }
};
