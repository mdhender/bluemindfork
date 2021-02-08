import { Flag } from "@bluemind/email";

import { DEFAULT_FOLDER_NAMES } from "./folders/helpers/DefaultFolders";
import {
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    CURRENT_MAILBOX,
    MESSAGE_IS_LOADED,
    MAILSHARE_FOLDERS,
    MAILSHARE_KEYS,
    MAILSHARE_ROOT_FOLDERS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_MAILBOX_KEY,
    MY_OUTBOX,
    MY_SENT,
    MY_TRASH
} from "~getters";
import { SET_ACTIVE_FOLDER } from "~mutations";
import { compare } from "../model/folder";

export const state = {
    activeFolder: undefined
};

export const mutations = {
    [SET_ACTIVE_FOLDER]: (state, { key }) => {
        state.activeFolder = key;
    }
};

const { INBOX, OUTBOX, DRAFTS, SENT, TRASH } = DEFAULT_FOLDER_NAMES;

export const getters = {
    [CURRENT_MAILBOX]: state => state.mailboxes[state.folders[state.activeFolder].mailboxRef.key],
    [ALL_SELECTED_MESSAGES_ARE_UNREAD]: (state, getters) => allSelectedMessageAreNot(state, getters, Flag.SEEN),
    [ALL_SELECTED_MESSAGES_ARE_READ]: (state, getters) => allSelectedMessageAre(state, getters, Flag.SEEN),
    [ALL_SELECTED_MESSAGES_ARE_FLAGGED]: (state, getters) => allSelectedMessageAre(state, getters, Flag.FLAGGED),
    [ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]: (state, getters) => allSelectedMessageAreNot(state, getters, Flag.FLAGGED),
    [ALL_MESSAGES_ARE_SELECTED]: ({ selection, messageList }) =>
        selection.length > 0 && selection.length === messageList.messageKeys.length,
    [MAILSHARE_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MAILSHARE_KEYS].includes(folder.mailboxRef.key)),
    [MY_MAILBOX_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MY_MAILBOX_KEY] === folder.mailboxRef.key),
    [MY_MAILBOX_ROOT_FOLDERS]: (state, { MY_MAILBOX_FOLDERS }) =>
        MY_MAILBOX_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [MAILSHARE_ROOT_FOLDERS]: (state, { MAILSHARE_FOLDERS }) =>
        MAILSHARE_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [MY_INBOX]: myGetterFor(INBOX),
    [MY_OUTBOX]: myGetterFor(OUTBOX),
    [MY_DRAFTS]: myGetterFor(DRAFTS),
    [MY_SENT]: myGetterFor(SENT),
    [MY_TRASH]: myGetterFor(TRASH)
};

function myGetterFor(folderName) {
    return ({ folders }, getters) =>
        Object.values(folders).find(
            folder => folder.mailboxRef.key === getters[MY_MAILBOX_KEY] && folder.imapName === folderName
        );
}

function allSelectedMessageAre({ selection, messages }, getters, flag) {
    if (selection.length > 0) {
        return selection.every(key => !getters[MESSAGE_IS_LOADED](key) || messages[key].flags.includes(flag));
    }
    return false;
}

function allSelectedMessageAreNot({ selection, messages }, getters, flag) {
    if (selection.length > 0) {
        return selection.every(key => !getters[MESSAGE_IS_LOADED](key) || !messages[key].flags.includes(flag));
    }
    return false;
}
