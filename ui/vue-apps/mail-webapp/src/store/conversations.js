import sortedIndexBy from "lodash.sortedindexby";

import Vue from "vue";

import { inject } from "@bluemind/inject";
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
    REMOVE_CONVERSATIONS
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
const { createOnlyMetadata, isFlagged, isUnread, messageKey } = messageUtils;
const {
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
    [REMOVE_CONVERSATIONS]: withAlertOrNot(removeConversations, "REMOVE", "", "RemoveConversations", 1)
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
    [CONVERSATION_MESSAGE_BY_KEY]: (state, { MY_SENT, MY_TRASH }) => key => {
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
        return {
            subject: messages[0]?.subject,
            from: messages[0]?.from,
            to: messages[0]?.to,
            cc: messages[0]?.cc,
            bcc: messages[0]?.bcc,
            key,
            size: messages.length,
            remoteRef: conversation.remoteRef,
            folderRef: conversation.folderRef,
            ...reducedMetadata(conversation.folderRef.key, messages),
            messages: messages.map(m => m.key)
        };
    },
    [CURRENT_CONVERSATION_METADATA]: (state, getters) => getters.CONVERSATION_METADATA(state.currentConversation)
};

function reducedMetadata(folderKey, messages) {
    let unreadCount = 0,
        flags = messages.length > 0 ? new Set([Flag.SEEN]) : new Set(),
        loading = messages.length > 0 ? LoadingStatus.LOADING : LoadingStatus.ERROR,
        hasAttachment = false,
        hasICS = false,
        preview,
        date = -1;
    messages.forEach(m => {
        if (m.folderRef.key === folderKey) {
            if (isUnread(m)) {
                unreadCount++;
                flags.delete(Flag.SEEN);
            }
            if (isFlagged(m)) {
                flags.add(Flag.FLAGGED);
            }
        }

        m.flags?.forEach(flag => [Flag.ANSWERED, Flag.FORWARDED].includes(flag) && flags.add(flag));

        if (m.composing || (m.loading === LoadingStatus.LOADED && m.folderRef.key === folderKey)) {
            loading = LoadingStatus.LOADED;
        }
        if (m.hasAttachment) {
            hasAttachment = true;
        }
        if (m.hasICS) {
            hasICS = true;
        }
        if (m.folderRef.key === folderKey && m.date > date) {
            preview = m.preview;
            date = m.date;
        }
    });

    return { unreadCount, flags: Array.from(flags), loading, hasAttachment, hasICS, preview, date };
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

async function fetchConversationIfNotLoaded({ commit, state }, { uid, folder, conversationsActivated }) {
    const key = messageKey(uid, folder.key);
    if (!state.conversationByKey[key]) {
        let refs;
        if (conversationsActivated) {
            const { value } = await inject("MailConversationPersistence", folder.mailboxRef.uid).getComplete(uid);
            refs = value.messageRefs;
        } else {
            uid = Number(uid);
            refs = [{ itemId: uid, folderUid: folder.remoteRef.uid }];
        }
        const conversation = createConversationStub(uid, FolderAdaptor.toRef(folder));
        conversation.loading = LoadingStatus.LOADING;
        const conversationRef = { key: conversation.key, uid };
        const messages = refs.map(({ itemId: internalId, folderUid }) =>
            createOnlyMetadata({ internalId, folder: FolderAdaptor.toRef(folderUid), conversationRef })
        );
        commit(ADD_CONVERSATIONS, { conversations: [conversation] });
        commit(ADD_MESSAGES, { messages, preserve: true });
    }
    return state.conversationByKey[key];
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
            const key = messageKey(raw.uid, folder.key);
            const conversationRef = { key, uid: raw.uid };
            raw.value.messageRefs.forEach(({ itemId, folderUid }) => {
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
        conversations.forEach(conversation => {
            const conversationRef = { key: conversation.key, uid: conversation.uid };
            messages.push(
                createOnlyMetadata({
                    internalId: conversation.remoteRef.uid,
                    folder: conversation.folderRef,
                    conversationRef
                })
            );
        });
    }
    commit(ADD_MESSAGES, { messages, preserve: true });
    // Should be set before multipleGet, but is set after to prevent batch reload of the reactive system.
    // Will be fixed when CONVERSATION_METADATA will stored in state instead of a dynamic getter.
    commit(SET_CONVERSATIONS_LOADING_STATUS, { conversations, status: LoadingStatus.LOADING });
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

async function moveConversations({ getters, commit }, { conversations, mailbox, folder, conversationsActivated }) {
    conversations = ensureArray(conversations);
    const messages = messagesInConversationFolder(getters, conversations);
    commit(REMOVE_CONVERSATIONS, conversations);
    try {
        conversationsActivated
            ? await apiConversations.move(conversations, folder, mailbox)
            : await apiMessages.move(messages, folder);
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
            ? await apiConversations.addFlag(conversations, Flag.DELETED, mailbox)
            : await apiMessages.addFlag(messages, Flag.DELETED);
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
        const conversation = conversationByKey[message.conversationRef.key];
        const index = conversation?.messages.indexOf(message.key);
        if (index >= 0) {
            conversation.messages.splice(index, 1);
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
            : await apiMessages.addFlag(messages, flag);
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
            : await apiMessages.deleteFlag(messages, flag);
    } catch (e) {
        commit(ADD_FLAG, { messages, flag });
        throw e;
    }
}
