import sortedIndexBy from "lodash.sortedindexby";
import Vue from "vue";

import { inject } from "@bluemind/inject";
import { Flag } from "@bluemind/email";

import {
    ADD_CONVERSATIONS,
    ADD_MESSAGES,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SET_CURRENT_CONVERSATION,
    SET_CONVERSATION_LIST,
    SET_MESSAGES_LOADING_STATUS,
    UNSET_CURRENT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    SELECT_ALL_CONVERSATIONS
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
    MOVE_CONVERSATION_MESSAGES,
    MOVE_CONVERSATIONS,
    REMOVE_CONVERSATION_MESSAGES,
    REMOVE_CONVERSATIONS,
    REPLACE_DRAFT_MESSAGE
} from "~/actions";
import {
    CONVERSATION_MESSAGE_BY_KEY,
    CONVERSATION_METADATA,
    CONVERSATION_IS_LOADED,
    CURRENT_CONVERSATION_METADATA
} from "~/getters";
import {
    createConversationStubsFromRawConversations,
    firstMessageInConversationFolder,
    messagesInConversationFolder,
    conversationMustBeRemoved
} from "~/model/conversations";
import apiMessages from "./api/apiMessages";
import messages from "./messages";

import { withAlert } from "./helpers/withAlert";
import { LoadingStatus } from "~/model/loading-status";
import { isFlagged, isUnread, messageKey } from "~/model/message";
import apiFolders from "./api/apiFolders";

const state = {
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
    [MOVE_CONVERSATION_MESSAGES]: async ({ state, dispatch }, { conversation, messages, folder }) => {
        if (conversationMustBeRemoved(state, conversation, messages)) {
            await dispatch(MOVE_CONVERSATIONS, { conversations: [conversation], folder });
        } else {
            await dispatch(MOVE_MESSAGES, { messages, folder });
        }
    },
    [MOVE_CONVERSATIONS]: withAlertOrNot(moveConversations, "MOVE", "", "MoveMessages", 1),
    [REMOVE_CONVERSATION_MESSAGES]: async ({ state, dispatch }, { conversation, messages }) => {
        if (conversationMustBeRemoved(state, conversation, messages)) {
            await dispatch(REMOVE_CONVERSATIONS, { conversations: [conversation] });
        } else {
            await dispatch(REMOVE_MESSAGES, { messages });
        }
    },
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
    [SET_CONVERSATION_LIST]: (state, { conversations }) => {
        conversations.forEach(conversation => {
            if (state.conversationByKey[conversation.key]) {
                state.conversationByKey[conversation.key].messages = conversation.messages;
                state.conversationByKey[conversation.key].date = conversation.date;
            } else {
                Vue.set(state.conversationByKey, conversation.key, conversation);
            }
        });
    },
    [ADD_CONVERSATIONS]: (state, { conversations }) => {
        conversations.forEach(conversation => Vue.set(state.conversationByKey, conversation.key, conversation));
    },
    [REMOVE_CONVERSATIONS]: (state, conversations) => {
        conversations.forEach(conversation => Vue.delete(state.conversationByKey, conversation.key));
    },
    [ADD_MESSAGES]: (state, messages) => {
        const cache = {};
        messages.forEach(message => {
            cache[message.key] = message;
            const conversation = message.conversationRef
                ? state.conversationByKey[message.conversationRef.key]
                : undefined;
            if (conversation && !conversation.messages.includes(message.key)) {
                const bestIndexForInsertion = sortedIndexBy(conversation.messages, message.key, key =>
                    cache[key] ? cache[key].date : state.messages[key].date
                );
                conversation.messages.splice(bestIndexForInsertion, 0, message.key);
            }
        });
    },
    [REMOVE_MESSAGES]: (state, { messages }) => {
        messages.forEach(message => {
            const conversation = state.conversationByKey[message.conversationRef.key];
            if (conversation) {
                const index = sortedIndexBy(
                    conversation.messages,
                    message.key,
                    key => state.messages[key]?.date || Number.MAX_VALUE
                );
                if (conversation.messages[index] === message.key) {
                    conversation.messages.splice(index, 1);
                }
            }
        });
    },
    [SET_MESSAGES_LOADING_STATUS]: (state, messages) => {
        removeMessages(
            state,
            messages.filter(m => m.loading === LoadingStatus.ERROR)
        );
    },
    [UNSELECT_ALL_CONVERSATIONS]: state => {
        state.currentConversation = undefined;
    },
    [SELECT_ALL_CONVERSATIONS]: state => {
        state.currentConversation = undefined;
    }
};

const getters = {
    [CONVERSATION_IS_LOADED]: () => metadata => metadata?.loading === LoadingStatus.LOADED,
    [CONVERSATION_MESSAGE_BY_KEY]: (state, { MY_SENT, MY_TRASH }) => key => {
        let messages = [];
        const conversation = state.conversationByKey[key];
        if (conversation) {
            const conversationIsInTrash = conversation.folderRef.key === MY_TRASH.key;
            conversation.messages.forEach(key => {
                const message = state.messages[key];
                const messageIsInTrash = message.folderRef.key === MY_TRASH.key;
                const isSentDuplicatesIndex = isSentDuplicates(state, conversation, message, MY_SENT);
                if (
                    message &&
                    message.loading !== LoadingStatus.ERROR &&
                    isSentDuplicatesIndex === -1 &&
                    (!messageIsInTrash || conversationIsInTrash)
                ) {
                    messages.push(message);
                }
            });
        }
        return messages;
    },
    [CONVERSATION_METADATA]: (state, getters) => key => {
        const messages = getters.CONVERSATION_MESSAGE_BY_KEY(key);
        if (messages.length === 0) {
            return null;
        }
        return {
            subject: messages[0].subject,
            from: messages[0].from,
            to: messages[0].to,
            key,
            size: messages.length,
            date: messages[messages.length - 1].date,
            remoteRef: state.conversationByKey[key].remoteRef,
            folderRef: state.conversationByKey[key].folderRef,
            ...reducedMetadata(state.conversationByKey[key].folderRef.key, messages),
            messages: messages.map(m => m.key)
        };
    },
    [CURRENT_CONVERSATION_METADATA]: (state, getters) => {
        if (state.currentConversation) {
            return getters.CONVERSATION_METADATA(state.currentConversation.key);
        }
        return undefined;
    }
};

function reducedMetadata(folderKey, messages) {
    let unreadCount = 0,
        flags = new Set(),
        loading = LoadingStatus.LOADED;
    messages.forEach(m => {
        if (m.folderRef.key === folderKey) {
            if (isUnread(m)) {
                unreadCount++;
            } else if (unreadCount === 0) {
                flags.add(Flag.SEEN);
            }
            if (isFlagged(m)) {
                flags.add(Flag.FLAGGED);
            }
        }
        if (m.loading === LoadingStatus.NOT_LOADED || m.loading === LoadingStatus.LOADING) {
            loading = m.loading;
        }
    });
    return { unreadCount, flags: Array.from(flags), loading };
}

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

function info(payload) {
    const conversations = ensureArray(payload.conversations);
    return {
        count: conversations.length,
        onlyConversations: conversations.every(conversation => conversation.messages.length > 1),
        noAlert: payload.noAlert
    };
}

async function fetchConversationIfNotLoaded({ commit, state }, { uid, folder }) {
    const key = messageKey(uid, folder.key);
    if (!state.conversationByKey[key]) {
        const rawConversation = await inject("MailConversationPersistence").getComplete(uid);
        const { conversations, messages } = createConversationStubsFromRawConversations([rawConversation], folder);
        commit(ADD_CONVERSATIONS, { conversations, messages });
    }
    return state.conversationByKey[key];
}

function markConversationsAsRead({ getters, dispatch }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(ADD_FLAG, { messages, flag: Flag.SEEN });
}

function markConversationsAsUnread({ getters, dispatch }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(DELETE_FLAG, { messages, flag: Flag.SEEN });
}

function markConversationsAsFlagged({ getters, dispatch }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = firstMessageInConversationFolder(getters, conversations);
    dispatch(ADD_FLAG, { messages, flag: Flag.FLAGGED });
}

function markConversationsAsUnflagged({ getters, dispatch }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    dispatch(DELETE_FLAG, { messages, flag: Flag.FLAGGED });
}

async function moveConversations({ getters, commit }, { conversations, folder }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        await apiMessages.move(messages, folder);
    } catch {
        commit(ADD_CONVERSATIONS, { conversations, messages });
    }
}

async function removeConversations({ getters, commit }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        await apiMessages.multipleDeleteById(messages);
    } catch {
        commit(ADD_CONVERSATIONS, { conversations, messages });
    }
}

function replaceDraftMessage({ commit }, { draft, message }) {
    commit(ADD_MESSAGES, [message]);
    commit(REMOVE_MESSAGES, { messages: [draft] });
}

async function emptyFolder({ commit, state }, { folder, mailbox }) {
    const messagesToRemove = [];
    const conversationsToRemove = [];
    const conversationsToRemoveMessages = [];
    Object.keys(state.conversationByKey).forEach(key => {
        const conversation = state.conversationByKey[key];
        if (conversation.folderRef.key === folder.key) {
            conversationsToRemove.push(conversation);
            conversationsToRemoveMessages.push(...conversation.messages.map(key => state.messages[key]));
        } else {
            conversation.messages.forEach(messageKey => {
                if (state.messages[messageKey].folderRef.key === folder.key) {
                    messagesToRemove.push(state.messages[messageKey]);
                }
            });
        }
    });

    if (messagesToRemove) {
        commit(REMOVE_MESSAGES, { messages: messagesToRemove });
    }

    if (conversationsToRemove) {
        commit(REMOVE_CONVERSATIONS, conversationsToRemove);
    }

    try {
        await apiFolders.emptyFolder(mailbox, folder);
    } catch (e) {
        if (messagesToRemove) {
            commit(ADD_MESSAGES, messagesToRemove);
        }
        if (conversationsToRemove) {
            commit(ADD_CONVERSATIONS, {
                conversations: conversationsToRemove,
                messages: conversationsToRemoveMessages
            });
        }
        throw e;
    }
}

function removeMessages({ conversationByKey }, messages) {
    messages.forEach(message => {
        if (message.loading === LoadingStatus.ERROR) {
            const conversation = conversationByKey[message.conversationRef.key];
            const index = conversation.messages.indexOf(message.key);
            if (index >= 0) {
                conversation.messages.splice(index, 1);
            }
        }
    });
}

// returns -1 if message is not a sent duplicate, otherwise, returns its index in conversation
function isSentDuplicates(state, conversation, message, sentFolder) {
    if (message.folderRef.key === sentFolder.key && conversation.messages.length > 1) {
        const conversationMessages = conversation.messages.map(key => state.messages[key]);
        return conversationMessages.findIndex(
            convMessage => convMessage.key !== message.key && convMessage.messageId === message.messageId
        );
    }
    return -1;
}
