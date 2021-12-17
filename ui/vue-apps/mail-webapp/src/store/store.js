import { Flag } from "@bluemind/email";

import { Cache } from "~/utils/cache";

import { DEFAULT_FOLDER_NAMES } from "./folders/helpers/DefaultFolders";
import {
    ACTIVE_MESSAGE,
    ALL_CONVERSATIONS_ARE_SELECTED,
    ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_READ,
    ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED,
    ALL_SELECTED_CONVERSATIONS_ARE_UNREAD,
    ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE,
    CONVERSATIONS_ACTIVATED,
    CURRENT_MAILBOX,
    FOLDERS,
    IS_ACTIVE_MESSAGE,
    IS_CURRENT_CONVERSATION,
    MAILBOX_FOLDERS,
    MAILBOX_SENT,
    MAILBOX_TRASH,
    MAILSHARE_FOLDERS,
    MAILSHARE_KEYS,
    MAILSHARE_ROOT_FOLDERS,
    CONVERSATION_LIST_ALL_KEYS,
    MY_DRAFTS,
    MY_INBOX,
    MY_MAILBOX_FOLDERS,
    MY_MAILBOX_ROOT_FOLDERS,
    MY_MAILBOX,
    MY_OUTBOX,
    MY_SENT,
    MY_TEMPLATES,
    MY_TRASH,
    NEXT_CONVERSATION,
    SELECTION,
    SELECTION_FLAGS
} from "~/getters";
import { SET_ACTIVE_FOLDER, SET_MAIL_THREAD_SETTING } from "~/mutations";
import { compare, create } from "~/model/folder";
import { LoadingStatus } from "~/model/loading-status";
import { equal } from "~/model/message";

export const state = {
    activeFolder: undefined,
    mailThreadSetting: false
};

export const mutations = {
    [SET_ACTIVE_FOLDER]: (state, { key }) => {
        state.activeFolder = key;
    },
    [SET_MAIL_THREAD_SETTING]: (state, booleanValue) => {
        state.mailThreadSetting = booleanValue;
    }
};

const { INBOX, OUTBOX, DRAFTS, SENT, TRASH, TEMPLATES } = DEFAULT_FOLDER_NAMES;

const UNKNOWN = 0;
const ALL = 2;
const NONE = 4;

export const getters = {
    [CURRENT_MAILBOX]: state => state.mailboxes[state.folders[state.activeFolder]?.mailboxRef?.key],
    [ALL_SELECTED_CONVERSATIONS_ARE_UNREAD]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.SEEN] === NONE,
    [ALL_SELECTED_CONVERSATIONS_ARE_READ]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.SEEN] === ALL,
    [ALL_SELECTED_CONVERSATIONS_ARE_FLAGGED]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.FLAGGED] === ALL,
    [ALL_SELECTED_CONVERSATIONS_ARE_UNFLAGGED]: (state, { SELECTION_FLAGS }) => SELECTION_FLAGS[Flag.FLAGGED] === NONE,
    [ALL_SELECTED_CONVERSATIONS_ARE_WRITABLE]: (state, { CURRENT_MAILBOX }) => CURRENT_MAILBOX.writable,
    [ALL_CONVERSATIONS_ARE_SELECTED]: (state, { SELECTION_KEYS, CONVERSATION_LIST_ALL_KEYS }) =>
        SELECTION_KEYS.length > 0 && SELECTION_KEYS.length === CONVERSATION_LIST_ALL_KEYS.length,
    [CONVERSATIONS_ACTIVATED]: (state, { CONVERSATION_LIST_IS_SEARCH_MODE }) =>
        state.mailThreadSetting === "true" &&
        state.folders[state.activeFolder].allowConversations &&
        !CONVERSATION_LIST_IS_SEARCH_MODE,
    [MAILBOX_FOLDERS]: (state, getters) => {
        const foldersByMailbox = getters[FOLDERS].reduce(
            (cache, folder) => cache.get(folder.mailboxRef.key).push(folder) && cache,
            new Cache(() => [])
        );
        return ({ key }) => foldersByMailbox.get(key);
    },
    [MAILSHARE_FOLDERS]: (state, getters) => getters[MAILSHARE_KEYS].flatMap(key => getters[MAILBOX_FOLDERS]({ key })),
    [MY_MAILBOX_FOLDERS]: (state, getters) => getters[MAILBOX_FOLDERS](getters[MY_MAILBOX]),
    [MY_MAILBOX_ROOT_FOLDERS]: (state, getters) =>
        getters[MY_MAILBOX_FOLDERS].filter(({ parent }) => !parent).sort(compare),
    [MAILSHARE_ROOT_FOLDERS]: (state, getters) =>
        getters[MAILSHARE_FOLDERS].filter(({ parent }) => !parent).sort(compare),
    [MY_INBOX]: (state, getters) => mailboxGetterFor(INBOX)(state, getters)(getters[MY_MAILBOX]),
    [MY_OUTBOX]: (state, getters) => mailboxGetterFor(OUTBOX)(state, getters)(getters[MY_MAILBOX]),
    [MY_DRAFTS]: (state, getters) => mailboxGetterFor(DRAFTS)(state, getters)(getters[MY_MAILBOX]),
    [MY_SENT]: (state, getters) => mailboxGetterFor(SENT)(state, getters)(getters[MY_MAILBOX]),
    [MY_TEMPLATES]: (state, getters) => mailboxGetterFor(TEMPLATES)(state, getters)(getters[MY_MAILBOX]),
    [MY_TRASH]: (state, getters) => mailboxGetterFor(TRASH)(state, getters)(getters[MY_MAILBOX]),
    [MAILBOX_TRASH]: mailboxGetterFor(TRASH),
    [MAILBOX_SENT]: mailboxGetterFor(SENT),
    [ACTIVE_MESSAGE]: ({ conversations: { messages }, activeMessage }) => messages[activeMessage.key],
    [IS_ACTIVE_MESSAGE]: ({ activeMessage, conversations: { conversationByKey } }) => ({ key }) =>
        key === activeMessage.key || conversationByKey[key].messages?.includes(activeMessage.key),
    [IS_CURRENT_CONVERSATION]: ({ conversations: { currentConversation } }) => ({ key }) =>
        equal(key, currentConversation),
    [NEXT_CONVERSATION]: (state, { [CONVERSATION_LIST_ALL_KEYS]: keys, CONVERSATION_METADATA }) => conversations => {
        let nextConversation = null;
        if (keys.length > 1 && conversations.length) {
            const conversationKeys = conversations.map(c => c.key);
            const keyIndexes = keys
                .map((k, index) => (conversationKeys.includes(k) ? index : -1))
                .filter(index => index >= 0);
            if (keyIndexes.length > 0) {
                const lastKeyIndex = keyIndexes.pop();
                let nextKeyIndex = lastKeyIndex + 1;
                if (nextKeyIndex === keys.length) {
                    const firstKeyIndex = keyIndexes?.shift() || lastKeyIndex;
                    nextKeyIndex = firstKeyIndex === 0 ? 0 : firstKeyIndex - 1;
                }
                nextConversation = CONVERSATION_METADATA(keys[nextKeyIndex]);
            }
        }
        return nextConversation;
    },
    [SELECTION]: (state, { CONVERSATION_METADATA, SELECTION_KEYS }) =>
        SELECTION_KEYS.map(key => CONVERSATION_METADATA(key)),
    [SELECTION_FLAGS]: (state, { CONVERSATION_METADATA, SELECTION_KEYS }) => {
        const meta = { [Flag.SEEN]: ALL | NONE, [Flag.FLAGGED]: ALL | NONE };
        for (let i = 0; i < SELECTION_KEYS.length && (meta[Flag.SEEN] || meta[Flag.FLAGGED]); i++) {
            const { flags, loading } = CONVERSATION_METADATA(SELECTION_KEYS[i]);
            if (loading !== LoadingStatus.LOADED) {
                return { [Flag.SEEN]: UNKNOWN, [Flag.FLAGGED]: UNKNOWN };
            }
            meta[Flag.SEEN] = meta[Flag.SEEN] & (flags.includes(Flag.SEEN) ? ALL : NONE);
            meta[Flag.FLAGGED] = meta[Flag.FLAGGED] & (flags.includes(Flag.FLAGGED) ? ALL : NONE);
        }
        return meta;
    }
};

function mailboxGetterFor(name) {
    return (state, getters) => ({ key }) => {
        const mailbox = state.mailboxes[key];
        if (mailbox.loading === LoadingStatus.LOADED) {
            return getters[MAILBOX_FOLDERS]({ key }).find(folder => folder.default && folder.imapName === name);
        } else {
            return create(undefined, name, null, mailbox);
        }
    };
}
