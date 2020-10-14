import {
    createConversationStubsFromRawConversations,
    createConversationStubsFromSearchResult,
    createConversationStubsFromSortedIds
} from "~/model/conversations";
import apiMessages from "./api/apiMessages";
import { inject } from "@bluemind/inject";
import searchModule from "./search";
import { FETCH_CONVERSATION_LIST_KEYS, CONVERSATION_LIST_NEXT_PAGE, REFRESH_CONVERSATION_LIST_KEYS } from "~/actions";
import {
    CONVERSATION_LIST_ALL_KEYS,
    CONVERSATION_LIST_COUNT,
    CONVERSATION_LIST_FILTERED,
    CONVERSATION_LIST_FLAGGED_FILTER_ENABLED,
    CONVERSATION_LIST_HAS_NEXT,
    CONVERSATION_LIST_IS_LOADING,
    CONVERSATION_LIST_IS_REJECTED,
    CONVERSATION_LIST_IS_RESOLVED,
    CONVERSATION_LIST_TOTAL_PAGES,
    CONVERSATION_LIST_KEYS,
    CONVERSATION_LIST_UNREAD_FILTER_ENABLED
} from "~/getters";
import {
    CLEAR_CONVERSATION_LIST,
    MOVE_MESSAGES,
    REMOVE_CONVERSATIONS,
    REMOVE_MESSAGES,
    RESET_CONVERSATION_LIST_PAGE,
    SET_CONVERSATION_LIST,
    SET_CONVERSATION_LIST_FILTER,
    SET_CONVERSATION_LIST_STATUS,
    SET_CONVERSATION_LIST_PAGE
} from "~/mutations";

const PAGE_SIZE = 50;

export const ConversationListStatus = {
    IDLE: Symbol("idle"),
    LOADING: Symbol("loading"),
    ERROR: Symbol("error"),
    SUCCESS: Symbol("success")
};

export const ConversationListFilter = {
    ALL: "all",
    UNREAD: "unread",
    FLAGGED: "flagged"
};

const state = {
    _keys: [],
    currentPage: 0,
    status: ConversationListStatus.LOADING,
    filter: ConversationListFilter.ALL,
    _removed: []
};

const mutations = {
    [CLEAR_CONVERSATION_LIST]: state => {
        state._keys = [];
        state.currentPage = 0;
        state._removed = [];
    },
    [RESET_CONVERSATION_LIST_PAGE]: state => {
        state.currentPage = 0;
    },
    [SET_CONVERSATION_LIST]: (state, messages) => {
        state._removed = [];
        state._keys = messages.map(({ key }) => key);
    },
    [SET_CONVERSATION_LIST_STATUS]: (state, status) => {
        state.status = status;
    },
    [SET_CONVERSATION_LIST_FILTER]: (state, filter) => {
        state.filter = filter;
    },
    [SET_CONVERSATION_LIST_PAGE]: (state, page) => {
        state.currentPage = page;
    },
    // Hooks
    [MOVE_MESSAGES]: (state, { conversation, messages }) => {
        if (conversation) {
            const messageKeysToRemove = new Set(messages.map(message => message.key));
            const countOfMessageInConversationFolder = conversation.messages
                .filter(message => message.folderRef.key === conversation.folderRef.key)
                .filter(message => !messageKeysToRemove.has(message.key)).length;
            if (countOfMessageInConversationFolder === 0) {
                state._removed.push(conversation.key);
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
                state._removed.push(conversation.key);
            }
        }
    },
    [REMOVE_CONVERSATIONS]: (state, conversations) => {
        const keys = new Set(conversations.map(({ key }) => key));
        for (let i = 0; i < state._keys.length && keys.size > 0; i++) {
            if (keys.has(state._keys[i])) {
                keys.delete(state._keys[i]);
                state._removed.push(state._keys[i]);
            }
        }
    }
};

const actions = {
    async [FETCH_CONVERSATION_LIST_KEYS]({ commit, dispatch }, { folder, conversationsActivated }) {
        commit(SET_CONVERSATION_LIST_STATUS, ConversationListStatus.LOADING);
        try {
            await dispatch(REFRESH_CONVERSATION_LIST_KEYS, { folder, conversationsActivated });
            commit(SET_CONVERSATION_LIST_STATUS, ConversationListStatus.SUCCESS);
        } catch (e) {
            commit(SET_CONVERSATION_LIST_STATUS, ConversationListStatus.ERROR);
            throw e;
        }
    },
    async [REFRESH_CONVERSATION_LIST_KEYS]({ commit, state, getters }, { folder, conversationsActivated }) {
        let conversations;
        if (getters.CONVERSATION_LIST_IS_SEARCH_MODE) {
            conversations = await search(state, folder);
        } else {
            conversations = await list(state, folder, conversationsActivated);
        }
        commit(SET_CONVERSATION_LIST, conversations);
    },
    async [CONVERSATION_LIST_NEXT_PAGE]({ commit, state, getters }) {
        if (state.currentPage < getters.CONVERSATION_LIST_TOTAL_PAGES) {
            commit(SET_CONVERSATION_LIST_PAGE, state.currentPage + 1);
            return Promise.resolve(true);
        }
        return Promise.resolve(false);
    }
};

async function search({ filter, search }, folder) {
    let searchResult = await apiMessages.search(search, filter, folder);
    return createConversationStubsFromSearchResult(searchResult);
}

async function list(state, folder, conversationsActivated) {
    if (conversationsActivated) {
        const flagFilter = { mustNot: ["Deleted"] };
        const rawConversations = await inject("MailConversationPersistence").byFolder(folder.remoteRef.uid, flagFilter);
        return createConversationStubsFromRawConversations(rawConversations, folder.remoteRef);
    } else {
        let sortedIds = await apiMessages.sortedIds(state.filter, folder);
        return createConversationStubsFromSortedIds(sortedIds, folder);
    }
}

function getPage(page, keys, opt_start) {
    const end = Math.min(keys.length, page * PAGE_SIZE);
    const start = opt_start === undefined ? Math.max(0, (page - 1) * PAGE_SIZE) : opt_start;
    return keys.slice(start, end);
}

const getters = {
    [CONVERSATION_LIST_FILTERED]: ({ filter }) => filter && filter !== ConversationListFilter.ALL,
    [CONVERSATION_LIST_FLAGGED_FILTER_ENABLED]: ({ filter }) => filter === ConversationListFilter.FLAGGED,
    [CONVERSATION_LIST_HAS_NEXT]: ({ currentPage }, { CONVERSATION_LIST_TOTAL_PAGES }) =>
        currentPage < CONVERSATION_LIST_TOTAL_PAGES,
    [CONVERSATION_LIST_IS_LOADING]: ({ status }) => status === ConversationListStatus.LOADING,
    [CONVERSATION_LIST_COUNT]: (state, { CONVERSATION_LIST_ALL_KEYS }) => CONVERSATION_LIST_ALL_KEYS.length,
    [CONVERSATION_LIST_IS_REJECTED]: ({ status }) => status === ConversationListStatus.ERROR,
    [CONVERSATION_LIST_IS_RESOLVED]: ({ status }) => status === ConversationListStatus.SUCCESS,
    [CONVERSATION_LIST_ALL_KEYS]: ({ _keys, _removed }) => {
        if (_removed.length === 0) {
            return _keys;
        } else if (_removed.length <= _keys.length) {
            const removed = new Set(_removed);
            const result = [];
            for (let i = 0; i < _keys.length && removed.size > 0; i++) {
                removed.has(_keys[i]) ? removed.delete(_keys[i]) : result.push(_keys[i]);
            }
            return [...result, ..._keys.slice(result.length + _removed.length)];
        }
        return [];
    },
    [CONVERSATION_LIST_KEYS]: ({ currentPage }, { CONVERSATION_LIST_ALL_KEYS }) =>
        getPage(currentPage, CONVERSATION_LIST_ALL_KEYS, 0),
    [CONVERSATION_LIST_TOTAL_PAGES]: (s, { CONVERSATION_LIST_ALL_KEYS }) =>
        Math.ceil(CONVERSATION_LIST_ALL_KEYS.length / PAGE_SIZE),
    [CONVERSATION_LIST_UNREAD_FILTER_ENABLED]: ({ filter }) => filter === ConversationListFilter.UNREAD
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
