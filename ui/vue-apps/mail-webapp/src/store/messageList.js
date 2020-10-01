import { inject } from "@bluemind/inject";
import { createOnlyMetadata } from "../model/message";
import mutationTypes from "./mutationTypes";
import actionTypes from "./actionTypes";
import apiMessages from "./api/apiMessages";

export const MessageListStatus = {
    IDLE: Symbol("idle"),
    LOADING: Symbol("loading"),
    ERROR: Symbol("error"),
    SUCCESS: Symbol("success")
};

export const MessageListFilter = {
    ALL: "all",
    UNREAD: "unread",
    FLAGGED: "flagged"
};

const state = {
    messageKeys: [],
    status: MessageListStatus.IDLE,
    filter: MessageListFilter.ALL
};

const mutations = {
    [mutationTypes.CLEAR_MESSAGE_LIST]: state => {
        state.messageKeys = [];
    },
    [mutationTypes.REMOVE_MESSAGES]: (state, messageKeys) => {
        state.messageKeys = state.messageKeys.filter(key => !messageKeys.includes(key));
    },
    [mutationTypes.SET_MESSAGE_LIST]: (state, messages) => {
        state.messageKeys = messages.map(m => m.key);
    },
    [mutationTypes.SET_MESSAGE_LIST_STATUS]: (state, status) => {
        state.status = status;
    },
    [mutationTypes.SET_MESSAGE_LIST_FILTER]: (state, filter) => {
        state.filter = filter;
    }
};

const actions = {
    async [actionTypes.FETCH_FOLDER_MESSAGE_KEYS]({ commit }, { folder, filter, conversationsEnabled }) {
        commit(mutationTypes.SET_MESSAGE_LIST_STATUS, MessageListStatus.LOADING);
        //FIXME: should be set by component not here
        commit(mutationTypes.SET_MESSAGE_LIST_FILTER, filter);
        try {
            let ids = await apiMessages.sortedIds(filter, folder);
            ids = conversationsEnabled ? await conversationFilter(folder, ids) : ids;
            const messages = ids.map(id =>
                createOnlyMetadata({ internalId: id, folder: { key: folder.key, uid: folder.remoteRef.uid } })
            );
            commit(mutationTypes.SET_MESSAGE_LIST, messages);
            commit(mutationTypes.SET_MESSAGE_LIST_STATUS, MessageListStatus.SUCCESS);
        } catch (e) {
            commit(mutationTypes.SET_MESSAGE_LIST_STATUS, MessageListStatus.ERROR);
            throw e;
        }
    }
};

async function conversationFilter(folder, ids) {
    let conversations = await inject("MailConversationPersistence").byFolder(folder.uid);
    return (
        conversations
            // extract first message from each conversation matching one of ids
            .map(({ value: { messageIds } }) => {
                const message = messageIds.sort((a, b) => a.date - b.date).find(({ itemId }) => ids.includes(itemId));
                return message ? { itemId: message.itemId, date: message.date } : undefined;
            })
            .filter(Boolean)
            // order by creation date, newer message to older one
            .sort((a, b) => b.date - a.date)
            .map(m => m.itemId)
    );
}

const getters = {
    MESSAGE_LIST_IS_LOADING: ({ status }) => status === MessageListStatus.LOADING,
    MESSAGE_LIST_IS_RESOLVED: ({ status }) => status === MessageListStatus.SUCCESS,
    MESSAGE_LIST_IS_REJECTED: ({ status }) => status === MessageListStatus.ERROR,
    MESSAGE_LIST_COUNT: ({ messageKeys }) => messageKeys.length,
    MESSAGE_LIST_FILTERED: ({ filter }) => filter && filter !== MessageListFilter.ALL,
    MESSAGE_LIST_UNREAD_FILTER_ENABLED: ({ filter }) => filter === MessageListFilter.UNREAD,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED: ({ filter }) => filter === MessageListFilter.FLAGGED
};
export default {
    actions,
    mutations,
    state,
    getters
};
