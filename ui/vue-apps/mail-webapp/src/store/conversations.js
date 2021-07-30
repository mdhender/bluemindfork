import {
    ADD_CONVERSATIONS,
    ADD_MESSAGE_TO_CONVERSATION,
    ADD_MESSAGES,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    REMOVE_NEW_MESSAGE_FROM_CONVERSATION,
    SET_CURRENT_CONVERSATION,
    SET_CONVERSATION_LIST,
    SET_MESSAGES_LOADING_STATUS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import {
    ADD_FLAG,
    DELETE_FLAG,
    EMPTY_FOLDER,
    FETCH_CONVERSATION_IF_NOT_LOADED,
    MARK_CONVERSATIONS_AS_FLAGGED,
    MARK_CONVERSATIONS_AS_READ,
    MARK_CONVERSATIONS_AS_UNFLAGGED,
    MARK_CONVERSATIONS_AS_UNREAD,
    MOVE_CONVERSATIONS,
    MOVE_CONVERSATIONS_TO_TRASH,
    MOVE_MESSAGES_NO_ALERT,
    REMOVE_CONVERSATIONS,
    REPLACE_DRAFT_MESSAGE
} from "~/actions";
import {
    CONVERSATION_MESSAGE_BY_KEY,
    CONVERSATION_METADATA,
    CONVERSATION_IS_LOADED,
    CONVERSATION_IS_LOADING
} from "~/getters";
import {
    createConversationStubsFromRawConversations,
    firstMessageInConversationFolder,
    messagesInConversationFolder,
    sameConversation
} from "~/model/conversations";
import apiMessages from "./api/apiMessages";
import messages from "./messages";
import { Flag } from "@bluemind/email";
import Vue from "vue";
import { withAlert } from "./helpers/withAlert";
import { LoadingStatus } from "~/model/loading-status";
import { isFlagged, isUnread, messageKey } from "~/model/message";
import { inject } from "@bluemind/inject";
import apiFolders from "./api/apiFolders";

const state = {
    /** Conversations keyed by the first message's key. */
    conversationByKey: {},
    currentConversation: undefined
};

const actions = {
    [EMPTY_FOLDER]: withAlert(emptyFolder, EMPTY_FOLDER, "EmptyFolder"),
    [FETCH_CONVERSATION_IF_NOT_LOADED]: fetchConversationIfNotLoaded,
    [MARK_CONVERSATIONS_AS_READ]: withAlertOrNot(markConversationsAsRead, "MARK", "_AS_READ"),
    [MARK_CONVERSATIONS_AS_UNREAD]: withAlertOrNot(markConversationsAsUnread, "MARK", "_AS_UNREAD"),
    [MARK_CONVERSATIONS_AS_FLAGGED]: withAlertOrNot(markConversationsAsFlagged, "MARK", "_AS_FLAGGED"),
    [MARK_CONVERSATIONS_AS_UNFLAGGED]: withAlertOrNot(markConversationsAsUnflagged, "MARK", "_AS_UNFLAGGED"),
    [MOVE_CONVERSATIONS]: withAlertOrNot(moveConversations, "MOVE", "", "MoveMessages", 1),
    [MOVE_CONVERSATIONS_TO_TRASH]: withAlertOrNot(moveConversations, "MOVE", "_TO_TRASH", "MoveMessages", 1),
    [REMOVE_CONVERSATIONS]: withAlertOrNot(removeConversations, "REMOVE", "", "RemoveMessages", 1),
    [REPLACE_DRAFT_MESSAGE]: replaceDraftMessage
};

const mutations = {
    [SET_CURRENT_CONVERSATION]: (state, conversation) => {
        state.currentConversation = conversation;
    },
    [UNSET_CURRENT_CONVERSATION]: state => {
        state.currentConversation = null;
    },
    [SET_CONVERSATION_LIST]: (state, conversationArray) => {
        conversationArray.forEach(conversation => {
            const conversationInState = state.conversationByKey[conversation.key];
            if (!conversationInState || !sameConversation(conversationInState, conversation)) {
                Vue.set(state.conversationByKey, conversation.key, conversation);
            }
        });
    },
    [ADD_CONVERSATIONS]: (state, conversations) => {
        conversations.forEach(conversation => Vue.set(state.conversationByKey, conversation.key, conversation));
    },
    [REMOVE_CONVERSATIONS]: (state, conversations) => {
        conversations.forEach(conversation => Vue.delete(state.conversationByKey, conversation.key));
    },
    [ADD_MESSAGE_TO_CONVERSATION]: (state, { conversation, message }) => {
        if (conversation) {
            message.conversationRef = { id: conversation.conversationId, key: conversation.key };
            state.conversationByKey[conversation.key].messages.push(message);
        }
    },
    [MOVE_MESSAGES]: (state, { conversation, messages }) => {
        if (conversation) {
            const messageKeysToRemove = new Set(messages.map(message => message.key));
            const countOfMessageInConversationFolder = conversation.messages
                .filter(message => message.folderRef.key === conversation.folderRef.key)
                .filter(message => !messageKeysToRemove.has(message.key)).length;
            if (countOfMessageInConversationFolder === 0) {
                Vue.delete(state.conversationByKey, conversation.key);
            }
        }
    },
    [REMOVE_MESSAGES]: (state, { conversation, messages }) => {
        if (conversation) {
            const messageKeysToRemove = new Set(messages.map(message => message.key));
            const countOfMessageInConversationFolder = conversation.messages
                .filter(message => message.folderRef.key === conversation.folderRef.key)
                .filter(message => !messageKeysToRemove.has(message.key)).length;
            if (countOfMessageInConversationFolder === 0) {
                Vue.delete(state.conversationByKey, conversation.key);
            } else {
                removeMessagesFromConversation(state.conversationByKey[conversation.key], messages);
            }
        }
    },
    [REMOVE_NEW_MESSAGE_FROM_CONVERSATION]: (state, { conversation, message }) => {
        if (conversation) {
            removeMessagesFromConversation(state.conversationByKey[conversation.key], [message]);
        }
    },
    [SET_MESSAGES_LOADING_STATUS]: (state, messages) => {
        removeMessages(
            state,
            messages.filter(m => m.loading === LoadingStatus.ERROR)
        );
    }
};

const getters = {
    [CONVERSATION_IS_LOADED]: () => metadata => metadata?.loading === LoadingStatus.LOADED,

    [CONVERSATION_IS_LOADING]: () => metadata => metadata?.loading === LoadingStatus.LOADING,

    [CONVERSATION_MESSAGE_BY_KEY]: (state, getters) => key => {
        const conversation = state.conversationByKey[key];
        return (
            conversation?.messages
                ?.map(message => state.messages[message.key] || message)
                .filter(
                    message =>
                        message &&
                        (message.folderRef.key !== getters.MY_TRASH.key ||
                            conversation.folderRef.key === getters.MY_TRASH.key) &&
                        message.loading !== LoadingStatus.ERROR
                ) || []
        );
    },
    [CONVERSATION_METADATA]: (state, getters) => key => {
        const messages = getters.CONVERSATION_MESSAGE_BY_KEY(key);
        if (messages.length === 0) {
            return null;
        }
        return {
            ...messages[0],
            key,
            size: messages.length,
            date: messages[messages.length - 1].date,
            remoteRef: state.conversationByKey[key].remoteRef,
            folderRef: state.conversationByKey[key].folderRef,
            ...reducedMetadata(state.conversationByKey[key].folderRef.key, messages),
            messages
        };
    }
};

function reducedMetadata(folderKey, messages) {
    const remaining = messages.slice(1);
    let flagsAndUnreadCount = updateFlagAndUnreadCount(folderKey, messages[0], {
        flags: new Set([Flag.SEEN]),
        unreadCount: 0
    });
    let loading = toLoad(messages[0]) ? LoadingStatus.LOADING : messages[0].loading;
    const metadata = remaining.reduce(
        ({ flags, unreadCount, loading }, message) => {
            flagsAndUnreadCount = updateFlagAndUnreadCount(folderKey, message, { flags, unreadCount });
            loading = updateLoadingStatus(message, loading);
            return { ...flagsAndUnreadCount, loading };
        },
        { ...flagsAndUnreadCount, loading }
    );
    return { ...metadata, flags: Array.from(metadata.flags) };
}

const toLoad = message => {
    return message.loading === LoadingStatus.NOT_LOADED || message.loading === LoadingStatus.LOADING;
};

const updateLoadingStatus = (message, loading) => {
    if (toLoad(message)) {
        loading = LoadingStatus.LOADING;
    } else if (loading !== LoadingStatus.LOADING && message.loading === LoadingStatus.LOADED) {
        loading = LoadingStatus.LOADED;
    }
    return loading;
};

const updateFlagAndUnreadCount = (conversationFolderKey, message, { flags, unreadCount }) => {
    if (message.folderRef.key === conversationFolderKey) {
        if (isUnread(message)) {
            flags.delete(Flag.SEEN);
            ++unreadCount;
        }
        if (isFlagged(message)) {
            flags.add(Flag.FLAGGED);
        }
    }
    return { flags, unreadCount };
};

export default {
    actions,
    getters,
    modules: {
        messages
    },
    mutations,
    state
};

function ensureArray(items) {
    return Array.isArray(items) ? items : [items];
}

function withAlertOrNot(action, actionPrefix, actionSuffix, renderer = "DefaultConversationAlert", minCount = 2) {
    return (store, payload) => {
        const { count, onlyConversations, noAlert } = info(payload, store.state);
        if (onlyConversations && !noAlert) {
            return withAlert(action, actionPrefix + "_CONVERSATIONS" + actionSuffix, renderer)(store, payload);
        } else if (count >= minCount && !noAlert) {
            return withAlert(action, actionPrefix + "_MESSAGES" + actionSuffix, renderer)(store, payload);
        } else {
            return action(store, payload);
        }
    };
}

function info(payload, state) {
    let conversations = ensureArray(payload.conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    return {
        count: conversations.length,
        onlyConversations: conversations.every(conversation => conversation.messages.length > 1),
        noAlert: payload.noAlert
    };
}

async function fetchConversationIfNotLoaded({ commit, state }, { conversationId, folder }) {
    const key = messageKey(conversationId, folder.key);
    if (!state.conversationByKey[key]) {
        const rawConversation = await inject("MailConversationPersistence").byConversationId(conversationId);
        const conversations = createConversationStubsFromRawConversations([rawConversation], folder.remoteRef);
        commit(ADD_CONVERSATIONS, conversations);
    }
    return state.conversationByKey[key];
}

function markConversationsAsRead({ getters, dispatch, state }, { conversations }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(ADD_FLAG, { messages, flag: Flag.SEEN });
}

function markConversationsAsUnread({ getters, dispatch, state }, { conversations }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(DELETE_FLAG, { messages, flag: Flag.SEEN });
}

function markConversationsAsFlagged({ getters, dispatch, state }, { conversations }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    const messages = firstMessageInConversationFolder(getters, conversations);
    dispatch(ADD_FLAG, { messages, flag: Flag.FLAGGED });
}

function markConversationsAsUnflagged({ getters, dispatch, state }, { conversations }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(DELETE_FLAG, { messages, flag: Flag.FLAGGED });
}

function moveConversations({ getters, dispatch, state }, { conversations, folder }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    conversations.forEach(conversation => {
        const messages = messagesInConversationFolder(getters, [conversation]);
        dispatch(MOVE_MESSAGES_NO_ALERT, { conversation, messages, folder });
    });
}

async function removeConversations({ getters, commit, state }, { conversations }) {
    conversations = ensureArray(conversations);
    conversations = conversations.map(c => state.conversationByKey[c.key]);
    const messages = conversations.flatMap(conversation => messagesInConversationFolder(getters, [conversation]));
    commit(REMOVE_CONVERSATIONS, conversations);
    await apiMessages.multipleDeleteById(messages);
}

function replaceDraftMessage({ commit, state }, { draft, message }) {
    commit(ADD_MESSAGES, [message]);
    commit(REMOVE_MESSAGES, { messages: [draft] });
    if (draft.conversationRef && state.conversationByKey[draft.conversationRef.key]) {
        commit(ADD_MESSAGE_TO_CONVERSATION, { conversation: { key: draft.conversationRef.key }, message });
        commit(REMOVE_NEW_MESSAGE_FROM_CONVERSATION, {
            conversation: { key: draft.conversationRef.key },
            message: draft
        });
    }
}

async function emptyFolder({ commit, state }, { folder, mailbox }) {
    const removedMessages = [];
    Object.keys(state.conversationByKey).forEach(key => {
        const conversation = state.conversationByKey[key];
        const messages = conversation.messages.filter(m => m.folderRef.key === folder.key);
        if (messages) {
            commit(REMOVE_MESSAGES, { conversation, messages });
            removedMessages.push(...messages);
        }
    });
    try {
        await apiFolders.emptyFolder(mailbox, folder);
    } catch (e) {
        commit(ADD_MESSAGES, removedMessages);
        throw e;
    }
}

function removeMessagesFromConversation(conversation, messages) {
    messages.forEach(message => {
        const messageIndex = conversation.messages.findIndex(m => m.key === message.key);
        if (messageIndex >= 0) {
            conversation.messages.splice(messageIndex, 1);
        }
    });
}

function removeMessages({ conversationByKey }, messages) {
    if (messages.length) {
        const messageKeys = messages.map(m => m.key);
        Object.values(conversationByKey).forEach(conversation => {
            conversation.messages = conversation.messages.filter(m => !messageKeys.includes(String(m.key)));
        });
    }
}
