import sortedIndexBy from "lodash.sortedindexby";

import Vue from "vue";

import { Flag } from "@bluemind/email";
import { conversationUtils, loadingStatusUtils, messageUtils, draftUtils } from "@bluemind/mail";

import {
    ADD_FLAG,
    ADD_CONVERSATIONS,
    ADD_MESSAGES,
    MOVE_MESSAGES,
    DELETE_FLAG,
    REMOVE_MESSAGES,
    RESET_CONVERSATIONS,
    SET_CONVERSATION_LIST,
    SET_CONVERSATIONS_LOADING_STATUS,
    SET_CURRENT_CONVERSATION,
    SET_MESSAGES_LOADING_STATUS,
    SET_SELECTION,
    SET_TEMPLATES_LIST,
    UNSELECT_ALL_CONVERSATIONS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import {
    EMPTY_FOLDER,
    FETCH_CONVERSATION_IF_NOT_LOADED,
    FETCH_CONVERSATIONS,
    MARK_CONVERSATIONS_AS_FLAGGED,
    MARK_CONVERSATIONS_AS_READ,
    MARK_CONVERSATIONS_AS_UNFLAGGED,
    MARK_CONVERSATIONS_AS_UNREAD,
    MOVE_CONVERSATION_MESSAGES,
    MOVE_CONVERSATIONS,
    REMOVE_CONVERSATION_MESSAGES,
    REMOVE_CONVERSATIONS,
    UNEXPUNGE
} from "~/actions";
import {
    CONVERSATION_MESSAGE_BY_KEY,
    CONVERSATION_METADATA,
    CONVERSATION_IS_LOADED,
    CURRENT_CONVERSATION_METADATA
} from "~/getters";
import apiMessages from "./api/apiMessages";
import apiConversations from "./api/apiConversations";

import messages from "./messages";

import { withAlert } from "./helpers/withAlert";
import apiFolders from "./api/apiFolders";
import { FolderAdaptor } from "./folders/helpers/FolderAdaptor";

const { getLastGeneratedNewMessageKey } = draftUtils;
const { createOnlyMetadata, messageKey } = messageUtils;
const {
    buildConversationMetadata,
    createConversationStub,
    firstMessageInConversationFolder,
    messagesInConversationFolder,
    conversationMustBeRemoved
} = conversationUtils;
const { LoadingStatus } = loadingStatusUtils;

const state = {
    conversationByKey: {},
    currentConversation: undefined
};

const actions = {
    [EMPTY_FOLDER]: withAlert(emptyFolder, EMPTY_FOLDER, "EmptyFolder"),
    [FETCH_CONVERSATION_IF_NOT_LOADED]: fetchConversationIfNotLoaded,
    [FETCH_CONVERSATIONS]: fetchConversations,
    [MARK_CONVERSATIONS_AS_READ]: withAlertOrNot(markConversationsAsRead, "MARK", "_AS_READ"),
    [MARK_CONVERSATIONS_AS_UNREAD]: withAlertOrNot(markConversationsAsUnread, "MARK", "_AS_UNREAD"),
    [MARK_CONVERSATIONS_AS_FLAGGED]: withAlertOrNot(markConversationsAsFlagged, "MARK", "_AS_FLAGGED"),
    [MARK_CONVERSATIONS_AS_UNFLAGGED]: withAlertOrNot(markConversationsAsUnflagged, "MARK", "_AS_UNFLAGGED"),
    [MOVE_CONVERSATION_MESSAGES]: async ({ state, dispatch }, { conversation, messages, folder }) => {
        if (conversationMustBeRemoved(state, conversation, messages)) {
            await dispatch(MOVE_CONVERSATIONS, { conversations: [conversation], folder, messages });
        } else {
            await dispatch(MOVE_MESSAGES, { messages, folder });
        }
    },
    [MOVE_CONVERSATIONS]: withAlertOrNot(moveConversations, "MOVE", "", "MoveConversations", 1),
    [REMOVE_CONVERSATION_MESSAGES]: async ({ state, dispatch }, { conversation, messages }) => {
        if (conversationMustBeRemoved(state, conversation, messages)) {
            await dispatch(REMOVE_CONVERSATIONS, { conversations: [conversation], messages });
        } else {
            await dispatch(REMOVE_MESSAGES, { messages });
        }
    },
    [REMOVE_CONVERSATIONS]: withAlertOrNot(removeConversations, "REMOVE", "", "RemoveConversations", 1),
    [UNEXPUNGE]: withAlert(unexpunge, UNEXPUNGE, "UnexpungeMessages")
};

const mutations = {
    [SET_CURRENT_CONVERSATION]: (state, { key }) => {
        state.currentConversation = key;
    },
    [UNSET_CURRENT_CONVERSATION]: state => {
        state.currentConversation = null;
    },
    [SET_CONVERSATION_LIST]: setConversations,
    [SET_TEMPLATES_LIST]: setConversations,
    [ADD_CONVERSATIONS]: (state, { conversations }) => {
        conversations.forEach(conversation => Vue.set(state.conversationByKey, conversation.key, conversation));
    },
    [REMOVE_CONVERSATIONS]: (state, conversations) => {
        conversations.forEach(conversation => Vue.delete(state.conversationByKey, conversation.key));
    },
    [SET_CONVERSATIONS_LOADING_STATUS]: (state, { conversations, loading }) => {
        conversations.forEach(({ key }) => {
            if (state.conversationByKey[key]) {
                state.conversationByKey[key].loading = loading;
            }
        });
    },
    //Hook
    [ADD_MESSAGES]: (state, { messages }) => {
        messages.forEach(message => {
            const conversation = state.conversationByKey[message.conversationRef?.key];
            if (conversation && !conversation.messages.includes(message.key)) {
                conversation.messages.push(message.key);
            }
        });
    },
    [REMOVE_MESSAGES]: (state, { messages }) => {
        messages.forEach(message => {
            const conversation = message.conversationRef && state.conversationByKey[message.conversationRef.key];
            if (conversation) {
                const index = conversation.messages.indexOf(message.key);
                if (index >= 0) {
                    conversation.messages.splice(index, 1);
                }
            }
        });
    },
    [RESET_CONVERSATIONS]: state => {
        state.conversationByKey = {};
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
    [SET_SELECTION]: state => {
        state.currentConversation = undefined;
    }
};

const getters = {
    [CONVERSATION_IS_LOADED]: () => metadata => metadata?.loading === LoadingStatus.LOADED,
    [CONVERSATION_MESSAGE_BY_KEY]:
        (state, { MY_SENT, MY_TRASH }) =>
        key => {
            let messages = [];
            const conversation = state.conversationByKey[key];
            if (conversation) {
                const conversationIsInTrash = conversation.folderRef.key === MY_TRASH.key;
                conversation.messages.forEach(key => {
                    const message = state.messages[key];
                    if (message) {
                        const messageIsInTrash = message.folderRef.key === MY_TRASH.key;
                        const sentDuplicateIndex = findSentDuplicateIndex(state, conversation, message, MY_SENT);
                        if (
                            message &&
                            message.loading !== LoadingStatus.ERROR &&
                            sentDuplicateIndex === -1 &&
                            (!messageIsInTrash || conversationIsInTrash)
                        ) {
                            messages.splice(sortedIndexBy(messages, message, "date"), 0, message);
                        }
                    }
                });
            }
            return messages;
        },
    [CONVERSATION_METADATA]: (state, getters) => key => {
        const conversation = state.conversationByKey[key];
        if (!conversation) {
            return null;
        }
        if (conversation.loading === LoadingStatus.NOT_LOADED) {
            return conversation;
        }
        const messages = getters.CONVERSATION_MESSAGE_BY_KEY(key);
        return buildConversationMetadata(key, conversation, messages);
    },
    [CURRENT_CONVERSATION_METADATA]: (state, getters) => getters.CONVERSATION_METADATA(state.currentConversation)
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

function info(payload) {
    const conversations = ensureArray(payload.conversations);
    return {
        count: conversations.length,
        onlyConversations: conversations.every(conversation => conversation.messages.length > 1),
        noAlert: payload.noAlert
    };
}

async function fetchConversationIfNotLoaded({ commit, dispatch, state }, { uid, folder, conversationsActivated }) {
    const key = messageKey(uid, folder.key);
    if (!state.conversationByKey[key]) {
        commit(ADD_CONVERSATIONS, { conversations: [createConversationStub(uid, FolderAdaptor.toRef(folder))] });
    }
    const conversation = state.conversationByKey[key];
    if (state.conversationByKey[key].loading === LoadingStatus.NOT_LOADED) {
        await dispatch(FETCH_CONVERSATIONS, { conversations: [conversation], folder, conversationsActivated });
    }
    return conversation;
}

async function fetchConversations({ commit, state }, { conversations, folder, conversationsActivated }) {
    let messages = [];
    if (conversationsActivated) {
        let newMessage;
        if (
            state.messages[getLastGeneratedNewMessageKey()] &&
            state.messages[getLastGeneratedNewMessageKey()].composing
        ) {
            newMessage = state.messages[getLastGeneratedNewMessageKey()];
        }
        (await apiConversations.multipleGet(conversations, folder.mailboxRef)).forEach(raw => {
            const key = messageKey(raw.conversationUid, folder.key);
            const conversationRef = { key, uid: raw.conversationUid };
            raw.messageRefs.forEach(({ itemId, folderUid }) => {
                if (newMessage?.folderRef.uid === folderUid && newMessage?.remoteRef.internalId === itemId) {
                    messages.push(newMessage);
                } else {
                    messages.push(
                        createOnlyMetadata({
                            internalId: itemId,
                            folder: FolderAdaptor.toRef(folderUid),
                            conversationRef
                        })
                    );
                }
            });
        });
    } else {
        conversations.forEach(conversation => messages.push(fakeConversationToMessage(conversation)));
    }
    commit(ADD_MESSAGES, { messages, preserve: true });
    // Should be set before multipleGet, but is set after to prevent batch reload of the reactive system.
    // Will be fixed when CONVERSATION_METADATA will stored in state instead of a dynamic getter.
    commit(SET_CONVERSATIONS_LOADING_STATUS, { conversations, loading: LoadingStatus.LOADING });
}

function markConversationsAsRead(store, { conversations, conversationsActivated, mailbox }) {
    return addFlag(store, conversations, conversationsActivated, mailbox, Flag.SEEN);
}

function markConversationsAsUnread(store, { conversations, conversationsActivated, mailbox }) {
    return deleteFlag(store, conversations, conversationsActivated, mailbox, Flag.SEEN);
}

function markConversationsAsFlagged(store, { conversations, conversationsActivated, mailbox }) {
    return addFlag(store, conversations, conversationsActivated, mailbox, Flag.FLAGGED);
}

function markConversationsAsUnflagged(store, { conversations, conversationsActivated, mailbox }) {
    return deleteFlag(store, conversations, conversationsActivated, mailbox, Flag.FLAGGED);
}

async function unexpunge({ commit, getters }, { conversations }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);

    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        await apiMessages.multipleUnexpungeById(conversations.map(fakeConversationToMessage));
    } catch (e) {
        commit(ADD_CONVERSATIONS, { conversations });
        commit(ADD_MESSAGES, { messages, preserve: true });
        throw e;
    }
}

async function moveConversations({ getters, commit }, { conversations, mailbox, folder, conversationsActivated }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        conversationsActivated
            ? await apiConversations.move(conversations, folder, mailbox)
            : await apiMessages.move(conversations.map(fakeConversationToMessage), folder);
    } catch (e) {
        commit(ADD_CONVERSATIONS, { conversations });
        commit(ADD_MESSAGES, { messages, preserve: true });
        throw e;
    }
}

async function removeConversations({ getters, commit }, { conversations, conversationsActivated, mailbox }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        conversationsActivated
            ? await apiConversations.multipleDeleteById(conversations, mailbox)
            : await apiMessages.multipleDeleteById(conversations.map(fakeConversationToMessage));
    } catch (e) {
        commit(ADD_CONVERSATIONS, { conversations });
        commit(ADD_MESSAGES, { messages, preserve: true });
        throw e;
    }
}

async function emptyFolder({ commit, state }, { folder, mailbox, deep }) {
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
                if (state.messages[messageKey]?.folderRef.key === folder.key) {
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
        await apiFolders.emptyFolder(mailbox, folder, deep);
    } catch (e) {
        if (messagesToRemove) {
            commit(ADD_MESSAGES, { messages: messagesToRemove });
        }
        if (conversationsToRemove) {
            commit(ADD_CONVERSATIONS, {
                conversations: conversationsToRemove
            });
            commit(ADD_MESSAGES, { messages: conversationsToRemoveMessages });
        }
        throw e;
    }
}

function removeMessages({ conversationByKey }, messages) {
    messages.forEach(message => {
        if (message.conversationRef?.key) {
            const conversation = conversationByKey[message.conversationRef.key];
            const index = conversation?.messages.indexOf(message.key);
            if (index >= 0) {
                conversation.messages.splice(index, 1);
            }
        }
    });
}

/**
 *  @returns {number} -1 if message is not a sent duplicate, otherwise, returns its index in conversation
 */
function findSentDuplicateIndex(state, conversation, message, sentFolder) {
    if (message.messageId && message.folderRef.key === sentFolder.key && conversation.messages.length > 1) {
        const conversationMessages = conversation.messages.map(key => state.messages[key]);
        return conversationMessages.findIndex(
            convMessage => convMessage.key !== message.key && convMessage.messageId === message.messageId
        );
    }
    return -1;
}

function setConversations({ conversationByKey }, { conversations }) {
    conversations.forEach(conversation => {
        if (!conversationByKey[conversation.key]) {
            Vue.set(conversationByKey, conversation.key, conversation);
        }
    });
}

async function addFlag({ commit, getters }, conversations, conversationsActivated, mailbox, flag) {
    conversations = ensureArray(conversations);

    let messages;
    if (flag === Flag.FLAGGED) {
        messages = firstMessageInConversationFolder(getters, conversations);
    } else {
        messages = messagesInConversationFolder(getters, conversations);
    }
    messages = messages.filter(m => m && m.loading === LoadingStatus.LOADED && !m.flags.includes(flag));

    commit(ADD_FLAG, { messages, flag });
    try {
        conversationsActivated
            ? await apiConversations.addFlag(conversations, flag, mailbox)
            : await apiMessages.addFlag(conversations.map(fakeConversationToMessage), flag);
    } catch (e) {
        commit(DELETE_FLAG, { messages, flag });
        throw e;
    }
}

async function deleteFlag({ commit, getters }, conversations, conversationsActivated, mailbox, flag) {
    conversations = ensureArray(conversations);

    const messages = messagesInConversationFolder(getters, conversations).filter(
        m => m && m.loading === LoadingStatus.LOADED && m.flags.includes(flag)
    );

    commit(DELETE_FLAG, { messages, flag });
    try {
        conversationsActivated
            ? await apiConversations.deleteFlag(conversations, flag, mailbox)
            : await apiMessages.deleteFlag(conversations.map(fakeConversationToMessage), flag);
    } catch (e) {
        commit(ADD_FLAG, { messages, flag });
        throw e;
    }
}

function fakeConversationToMessage(conversation) {
    return createOnlyMetadata({
        internalId: parseInt(conversation.remoteRef.uid),
        folder: conversation.folderRef,
        conversationRef: { key: conversation.key, uid: conversation.conversationUid }
    });
}
