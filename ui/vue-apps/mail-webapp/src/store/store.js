import { Flag } from "@bluemind/email";

import { DEFAULT_FOLDER_NAMES } from "./folders/helpers/DefaultFolders";
import {
    ACTIVE_MESSAGE,
    ALL_CONVERSATIONS_ARE_SELECTED,
    ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_READ,
    ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CONVERSATION_IS_LOADED,
    CONVERSATION_METADATA,
    CURRENT_MAILBOX,
    IS_ACTIVE_MESSAGE,
    IS_CURRENT_CONVERSATION,
    MAILSHARE_FOLDERS,
    MAILSHARE_KEYS,
    MAILSHARE_ROOT_FOLDERS,
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_CONVERSATIONS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_KEY,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_MAILBOX,
    MY_OUTBOX,
    MY_SENT,
    MY_TRASH,
    NEXT_CONVERSATION,
    SELECTION,
    SELECTION_FLAGS
} from "~/getters";
import { SET_ACTIVE_FOLDER } from "~/mutations";
import { compare, create } from "~/model/folder";
import { LoadingStatus } from "~/model/loading-status";
import { equal } from "~/model/message";

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
    [ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]: (
        state,
        getters // TODO try to use SELECTION FLAGS, remove allSelectedConversationsAre
    ) => allSelectedConversationsAre(state, getters, Flag.FLAGGED),
    [ALL_SELECTED_CONVERSATIONS_ARE_READ]: (state, getters) => allSelectedConversationsAre(state, getters, Flag.SEEN),
    [ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]: (state, getters) =>
        allSelectedConversationsAreNot(state, getters, Flag.FLAGGED),
    [ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]: (state, getters) =>
        allSelectedConversationsAreNot(state, getters, Flag.SEEN),
    [ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE]: (state, { CURRENT_MAILBOX }) => CURRENT_MAILBOX.writable,
    [ALL_CONVERSATIONS_ARE_SELECTED]: (state, { SELECTION_KEYS, CONVERSATION_LIST_ALL_KEYS }) =>
        SELECTION_KEYS.length > 0 && SELECTION_KEYS.length === CONVERSATION_LIST_ALL_KEYS.length,
    [MAILSHARE_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MAILSHARE_KEYS].includes(folder.mailboxRef.key)),
    [MY_MAILBOX_FOLDERS]: ({ folders }, getters) =>
        Object.values(folders).filter(folder => getters[MY_MAILBOX_KEY] === folder.mailboxRef.key),
    [MY_MAILBOX_ROOT_FOLDERS]: (state, { MY_MAILBOX_FOLDERS }) =>
        MY_MAILBOX_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [MAILSHARE_ROOT_FOLDERS]: (state, { MAILSHARE_FOLDERS }) =>
        MAILSHARE_FOLDERS.filter(({ parent }) => !parent).sort(compare),
    [CONVERSATION_LIST_CONVERSATIONS]: (state, { CONVERSATION_LIST_KEYS, CONVERSATION_METADATA }) =>
        CONVERSATION_LIST_KEYS.map(key => CONVERSATION_METADATA(key)),
    [MY_INBOX]: myGetterFor(INBOX),
    [MY_OUTBOX]: myGetterFor(OUTBOX),
    [MY_DRAFTS]: myGetterFor(DRAFTS),
    [MY_SENT]: myGetterFor(SENT),
    [MY_TRASH]: myGetterFor(TRASH),
    [ACTIVE_MESSAGE]: ({ conversations: { messages }, activeMessage }) => messages[activeMessage.key],
    [IS_ACTIVE_MESSAGE]: ({ conversations: { messages } }, { ACTIVE_MESSAGE }) => ({ key }) =>
        equal(messages[key], ACTIVE_MESSAGE),
    [IS_CURRENT_CONVERSATION]: ({ conversations: { currentConversation } }) => conversation =>
        equal(conversation, currentConversation),
    [NEXT_CONVERSATION]: (
        { conversations: { conversationByKey, currentConversation } },
        { [CONVERSATION_LIST_ALL_KEYS]: keys }
    ) => {
        if (currentConversation && keys.length > 1) {
            for (let i = 0; i < keys.length; i++) {
                if (currentConversation.key === keys[i]) {
                    return conversationByKey[i + 1 < keys.length ? keys[i + 1] : keys[i - 1]];
                }
            }
            return equal(currentConversation, conversationByKey[keys[0]])
                ? conversationByKey[keys[1]]
                : conversationByKey[keys[0]];
        }
        return null;
    },
    [SELECTION]: (state, { CONVERSATION_METADATA, SELECTION_KEYS }) =>
        SELECTION_KEYS.map(key => CONVERSATION_METADATA(key)),
    [SELECTION_FLAGS]: ({ conversations: { messages } }, { SELECTION_KEYS }) => {
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
function allSelectedConversationsAre({ selection }, getters, flag) {
    if (selection.length > 0) {
        return selection
            .map(key => getters[CONVERSATION_METADATA](key))
            .filter(Boolean)
            .every(metadata => !getters[CONVERSATION_IS_LOADED](metadata) || metadata.flags.includes(flag));
    }
    return false;
}
function allSelectedConversationsAreNot({ selection }, getters, flag) {
    if (selection.length > 0) {
        return selection
            .map(key => getters[CONVERSATION_METADATA](key))
            .filter(Boolean)
            .every(metadata => !getters[CONVERSATION_IS_LOADED](metadata) || !metadata.flags.includes(flag));
    }
    return false;
}
