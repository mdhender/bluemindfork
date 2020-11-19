import { inject } from "@bluemind/inject";
import { createOnlyMetadata } from "../model/message";
import apiMessages from "./api/apiMessages";
import { FolderAdaptor } from "./folders/helpers/FolderAdaptor";
import searchModule from "./search";
import { FETCH_MESSAGE_LIST_KEYS, REFRESH_MESSAGE_LIST_KEYS } from "~actions";
import {
    MESSAGE_LIST_COUNT,
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
    MESSAGE_LIST_IS_LOADING,
    MESSAGE_LIST_IS_REJECTED,
    MESSAGE_LIST_IS_RESOLVED,
    MESSAGE_LIST_UNREAD_FILTER_ENABLED
} from "~getters";
import {
    CLEAR_MESSAGE_LIST,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SET_MESSAGE_LIST,
    SET_MESSAGE_LIST_FILTER,
    SET_MESSAGE_LIST_STATUS
} from "~mutations";

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
    [CLEAR_MESSAGE_LIST]: state => {
        state.messageKeys = [];
    },
    [REMOVE_MESSAGES]: (state, messages) => {
        const keys = new Set();
        messages.forEach(({ key }) => keys.add(key));
        state.messageKeys = state.messageKeys.filter(key => !keys.has(key));
    },
    [MOVE_MESSAGES]: (state, { messages }) => {
        const keys = new Set();
        messages.forEach(({ key }) => keys.add(key));
        state.messageKeys = state.messageKeys.filter(key => !keys.has(key));
    },
    [SET_MESSAGE_LIST]: (state, messages) => {
        state.messageKeys = messages.map(m => m.key);
    },
    [SET_MESSAGE_LIST_STATUS]: (state, status) => {
        state.status = status;
    },
    [SET_MESSAGE_LIST_FILTER]: (state, filter) => {
        state.filter = filter;
    }
};

const actions = {
    async [FETCH_MESSAGE_LIST_KEYS]({ commit, dispatch }, { folder, conversationsEnabled }) {
        commit(SET_MESSAGE_LIST_STATUS, MessageListStatus.LOADING);
        try {
            await dispatch(REFRESH_MESSAGE_LIST_KEYS, { folder, conversationsEnabled });
            commit(SET_MESSAGE_LIST_STATUS, MessageListStatus.SUCCESS);
        } catch (e) {
            commit(SET_MESSAGE_LIST_STATUS, MessageListStatus.ERROR);
            throw e;
        }
    },
    async [REFRESH_MESSAGE_LIST_KEYS]({ commit, state, getters }, { folder, conversationsEnabled }) {
        const messages = getters.MESSAGE_LIST_IS_SEARCH_MODE
            ? await search(state, folder)
            : await list(state, folder, conversationsEnabled);
        commit(SET_MESSAGE_LIST, messages);
    }
};

async function search({ filter, search }, folder) {
    let results = await apiMessages.search(search, filter, folder);
    return results.map(({ id, folderRef }) => createOnlyMetadata({ internalId: id, folder: folderRef }));
}

async function list({ filter }, folder, conversation) {
    let ids = await apiMessages.sortedIds(filter, folder);
    ids = conversation ? await conversationFilter(folder, ids) : ids;
    return ids.map(id => createOnlyMetadata({ internalId: id, folder: FolderAdaptor.toRef(folder) }));
}

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
    [MESSAGE_LIST_IS_LOADING]: ({ status }) => status === MessageListStatus.LOADING,
    [MESSAGE_LIST_IS_RESOLVED]: ({ status }) => status === MessageListStatus.SUCCESS,
    [MESSAGE_LIST_IS_REJECTED]: ({ status }) => status === MessageListStatus.ERROR,
    [MESSAGE_LIST_COUNT]: ({ messageKeys }) => messageKeys.length,
    [MESSAGE_LIST_FILTERED]: ({ filter }) => filter && filter !== MessageListFilter.ALL,
    [MESSAGE_LIST_UNREAD_FILTER_ENABLED]: ({ filter }) => filter === MessageListFilter.UNREAD,
    [MESSAGE_LIST_FLAGGED_FILTER_ENABLED]: ({ filter }) => filter === MessageListFilter.FLAGGED
};
export default {
    actions,
    mutations,
    state,
    getters,
    modules: {
        search: searchModule
    }
};
