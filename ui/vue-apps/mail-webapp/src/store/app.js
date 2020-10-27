import { Flag } from "@bluemind/email";
import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    IS_MESSAGE_LOADED
} from "./types/getters";

export const state = {
    activeFolder: undefined
};

export const mutations = {
    SET_ACTIVE_FOLDER: (state, key) => {
        state.activeFolder = key;
    }
};

export const getters = {
    CURRENT_MAILBOX: state => state.mailboxes[state.folders[state.activeFolder].mailboxRef.key],
    [ALL_SELECTED_MESSAGES_ARE_UNREAD]: (state, getters) => allSelectedMessageAreNot(state, getters, Flag.SEEN),
    [ALL_SELECTED_MESSAGES_ARE_READ]: (state, getters) => allSelectedMessageAre(state, getters, Flag.SEEN),
    [ALL_SELECTED_MESSAGES_ARE_FLAGGED]: (state, getters) => allSelectedMessageAre(state, getters, Flag.FLAGGED),
    [ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]: (state, getters) => allSelectedMessageAreNot(state, getters, Flag.FLAGGED),
    [ALL_MESSAGES_ARE_SELECTED]: ({ selection, messageList }) =>
        selection.length > 0 && selection.length === messageList.messageKeys.length
};

function allSelectedMessageAre({ selection, messages }, getters, flag) {
    if (selection.length > 0) {
        return selection.every(key => !getters[IS_MESSAGE_LOADED](key) || messages[key].flags.includes(flag));
    }
    return false;
}

function allSelectedMessageAreNot({ selection, messages }, getters, flag) {
    if (selection.length > 0) {
        return selection.every(key => !getters[IS_MESSAGE_LOADED](key) || !messages[key].flags.includes(flag));
    }
    return false;
}
