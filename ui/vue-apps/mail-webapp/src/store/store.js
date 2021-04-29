import { Flag } from "@bluemind/email";

import { DEFAULT_FOLDER_NAMES } from "./folders/helpers/DefaultFolders";
import {
    ACTIVE_MESSAGE,
    ALL_MESSAGES_ARE_SELECTED,
    ALL_SELECTED_MESSAGES_ARE_FLAGGED,
    ALL_SELECTED_MESSAGES_ARE_READ,
    ALL_SELECTED_MESSAGES_ARE_UNFLAGGED,
    ALL_SELECTED_MESSAGES_ARE_UNREAD,
    ALL_SELECTED_MESSAGES_ARE_WRITABLE,
    CURRENT_MAILBOX,
    IS_ACTIVE_MESSAGE,
    MAILSHARE_FOLDERS,
    MAILSHARE_KEYS,
    MAILSHARE_ROOT_FOLDERS,
    MESSAGE_LIST_ALL_KEYS,
    MESSAGE_LIST_MESSAGES,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_KEY,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_MAILBOX,
    MY_OUTBOX,
    MY_SENT,
    MY_TRASH,
    NEXT_MESSAGE,
    SELECTION,
    SELECTION_FLAGS
} from "~getters";
import { SET_ACTIVE_FOLDER } from "~mutations";
import { compare, create } from "../model/folder";
import { LoadingStatus } from "../model/loading-status";
import { equal } from "../model/message";

export const state = {
    activeFolder: undefined
};

export const mutations = {
    [SET_ACTIVE_FOLDER]: (state, { key }) => {
        state.activeFolder = key;
    }
};

const { INBOX, OUTBOX, DRAFTS, SENT, TRASH } = DEFAULT_FOLDER_NAMES;

const UNKNOWN = 0;
const ALL = 2;
const NONE = 4;

export const getters = {
    [CURRENT_MAILBOX]: state => state.mailboxes[state.folders[state.activeFolder]?.mailboxRef?.key],
    [ALL_SELECTED_MESSAGES_ARE_UNREAD]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.SEEN] === NONE,
    [ALL_SELECTED_MESSAGES_ARE_READ]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.SEEN] === ALL,
    [ALL_SELECTED_MESSAGES_ARE_FLAGGED]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.FLAGGED] === ALL,
    [ALL_SELECTED_MESSAGES_ARE_UNFLAGGED]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.FLAGGED] === NONE,
    [ALL_SELECTED_MESSAGES_ARE_WRITABLE]: (state, { CURRENT_MAILBOX }) => CURRENT_MAILBOX.writable,
    [ALL_MESSAGES_ARE_SELECTED]: (state, { SELECTION_KEYS, MESSAGE_LIST_ALL_KEYS }) =>
        SELECTION_KEYS.length > 0 && SELECTION_KEYS.length === MESSAGE_LIST_ALL_KEYS.length,
    [MAILSHARE_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MAILSHARE_KEYS].includes(folder.mailboxRef.key)),
    [MY_MAILBOX_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MY_MAILBOX_KEY] === folder.mailboxRef.key),
    [MY_MAILBOX_ROOT_FOLDERS]: (state, { MY_MAILBOX_FOLDERS }) =>
        MY_MAILBOX_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [MAILSHARE_ROOT_FOLDERS]: (state, { MAILSHARE_FOLDERS }) =>
        MAILSHARE_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [MESSAGE_LIST_MESSAGES]: ({ messages }, { MESSAGE_LIST_KEYS }) => MESSAGE_LIST_KEYS.map(key => messages[key]),
    [MY_INBOX]: myGetterFor(INBOX),
    [MY_OUTBOX]: myGetterFor(OUTBOX),
    [MY_DRAFTS]: myGetterFor(DRAFTS),
    [MY_SENT]: myGetterFor(SENT),
    [MY_TRASH]: myGetterFor(TRASH),
    [ACTIVE_MESSAGE]: ({ messages, activeMessage }) => messages[activeMessage.key],
    [IS_ACTIVE_MESSAGE]: ({ messages }, { ACTIVE_MESSAGE }) => ({ key }) => equal(messages[key], ACTIVE_MESSAGE),
    [NEXT_MESSAGE]: ({ messages }, { [MESSAGE_LIST_ALL_KEYS]: keys, ACTIVE_MESSAGE }) => {
        if (ACTIVE_MESSAGE && keys.length > 1) {
            for (let i = 0; i < keys.length; i++) {
                if (ACTIVE_MESSAGE.key === keys[i]) {
                    return messages[i + 1 < keys.length ? keys[i + 1] : keys[i - 1]];
                }
            }
            return equal(ACTIVE_MESSAGE, messages[keys[0]]) ? messages[keys[1]] : messages[keys[0]];
        }
        return null;
    },
    [SELECTION]: ({ messages }, { SELECTION_KEYS }) => SELECTION_KEYS.map(key => messages[key]),
    [SELECTION_FLAGS]: ({ messages }, { SELECTION_KEYS }) => {
        const meta = { [Flag.SEEN]: ALL | NONE, [Flag.FLAGGED]: ALL | NONE };
        for (let i = 0; i < SELECTION_KEYS.length && (meta[Flag.SEEN] || meta[Flag.FLAGGED]); i++) {
            const { flags, loading } = messages[SELECTION_KEYS[i]];
            if (loading !== LoadingStatus.LOADED) {
                return { [Flag.SEEN]: UNKNOWN, [Flag.FLAGGED]: UNKNOWN };
            }
            meta[Flag.SEEN] = meta[Flag.SEEN] & (flags.includes(Flag.SEEN) ? ALL : NONE);
            meta[Flag.FLAGGED] = meta[Flag.FLAGGED] & (flags.includes(Flag.FLAGGED) ? ALL : NONE);
        }
        return meta;
    }
};

function myGetterFor(name) {
    return (state, getters) => {
        if (getters[MY_MAILBOX].loading === LoadingStatus.LOADED) {
            return getters[MY_MAILBOX_ROOT_FOLDERS].find(({ imapName }) => imapName === name);
        } else {
            return create(undefined, name, null, getters[MY_MAILBOX]);
        }
    };
}
