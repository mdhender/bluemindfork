import { inject } from "@bluemind/inject";
import { createOnlyMetadata } from "~model/message";
import apiMessages from "./api/apiMessages";
import { FolderAdaptor } from "./folders/helpers/FolderAdaptor";
import searchModule from "./search";
import { FETCH_MESSAGE_LIST_KEYS, MESSAGE_LIST_NEXT_PAGE, REFRESH_MESSAGE_LIST_KEYS } from "~actions";
import {
    MESSAGE_LIST_ALL_KEYS,
    MESSAGE_LIST_COUNT,
    MESSAGE_LIST_FILTERED,
    MESSAGE_LIST_FLAGGED_FILTER_ENABLED,
    MESSAGE_LIST_HAS_NEXT,
    MESSAGE_LIST_IS_LOADING,
    MESSAGE_LIST_IS_REJECTED,
    MESSAGE_LIST_IS_RESOLVED,
    MESSAGE_LIST_TOTAL_PAGES,
    MESSAGE_LIST_KEYS,
    MESSAGE_LIST_UNREAD_FILTER_ENABLED
} from "~getters";
import {
    ADD_MESSAGES,
    CLEAR_MESSAGE_LIST,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    RESET_MESSAGE_LIST_PAGE,
    SET_MESSAGE_LIST,
    SET_MESSAGE_LIST_FILTER,
    SET_MESSAGE_LIST_STATUS,
    SET_MESSAGE_LIST_PAGE
} from "~mutations";

const PAGE_SIZE = 50;

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
    _keys: [],
    currentPage: 0,
    status: MessageListStatus.LOADING,
    filter: MessageListFilter.ALL,
    _removed: []
};

const mutations = {
    [CLEAR_MESSAGE_LIST]: state => {
        state._keys = [];
        state.currentPage = 0;
        state._removed = [];
    },
    [RESET_MESSAGE_LIST_PAGE]: state => {
        state.currentPage = 0;
    },
    [SET_MESSAGE_LIST]: (state, messages) => {
        state._removed = [];
        state._keys = messages.map(({ key }) => key);
    },
    [SET_MESSAGE_LIST_STATUS]: (state, status) => {
        state.status = status;
    },
    [SET_MESSAGE_LIST_FILTER]: (state, filter) => {
        state.filter = filter;
    },
    [SET_MESSAGE_LIST_PAGE]: (state, page) => {
        state.currentPage = page;
    },
    // Hooks
    [ADD_MESSAGES]: (state, messages) => {
        if (state._removed.length > 0) {
            const keys = new Set(messages.map(({ key }) => key));
            for (let i = state._removed.length - 1; i >= 0 && keys.size > 0; i--) {
                if (keys.has(state._removed[i])) {
                    keys.delete(state._removed[i]);
                    state._removed.splice(i, 1);
                }
            }
        }
    },
    [REMOVE_MESSAGES]: (state, messages) => {
        const keys = new Set(messages.map(({ key }) => key));
        for (let i = 0; i < state._keys.length && keys.size > 0; i++) {
            if (keys.has(state._keys[i])) {
                keys.delete(state._keys[i]);
                state._removed.push(state._keys[i]);
            }
        }
    },
    [MOVE_MESSAGES]: (state, { messages }) => {
        const moved = new Set(messages.map(({ key }) => key));
        const removed = new Set(state._removed);
        for (let i = 0; i < state._keys.length && moved.size > 0; i++) {
            if (moved.has(state._keys[i])) {
                moved.delete(state._keys[i]);
                if (removed.has(state._keys[i])) {
                    removed.delete(state._keys[i]);
                } else {
                    removed.add(state._keys[i]);
                }
            }
        }
        state._removed = Array.from(removed);
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
    },
    async [MESSAGE_LIST_NEXT_PAGE]({ commit, state, getters }) {
        if (state.currentPage < getters.MESSAGE_LIST_TOTAL_PAGES) {
            commit(SET_MESSAGE_LIST_PAGE, state.currentPage + 1);
            return Promise.resolve(true);
        }
        return Promise.resolve(false);
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

function getPage(page, keys, opt_start) {
    const end = Math.min(keys.length, page * PAGE_SIZE);
    const start = opt_start === undefined ? Math.max(0, (page - 1) * PAGE_SIZE) : opt_start;
    return keys.slice(start, end);
}

const getters = {
    [MESSAGE_LIST_FILTERED]: ({ filter }) => filter && filter !== MessageListFilter.ALL,
    [MESSAGE_LIST_FLAGGED_FILTER_ENABLED]: ({ filter }) => filter === MessageListFilter.FLAGGED,
    [MESSAGE_LIST_HAS_NEXT]: ({ currentPage }, { MESSAGE_LIST_TOTAL_PAGES }) => currentPage < MESSAGE_LIST_TOTAL_PAGES,
    [MESSAGE_LIST_IS_LOADING]: ({ status }) => status === MessageListStatus.LOADING,
    [MESSAGE_LIST_COUNT]: (state, { MESSAGE_LIST_ALL_KEYS }) => MESSAGE_LIST_ALL_KEYS.length,
    [MESSAGE_LIST_IS_REJECTED]: ({ status }) => status === MessageListStatus.ERROR,
    [MESSAGE_LIST_IS_RESOLVED]: ({ status }) => status === MessageListStatus.SUCCESS,
    [MESSAGE_LIST_ALL_KEYS]: ({ _keys, _removed }) => {
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
    [MESSAGE_LIST_KEYS]: ({ currentPage }, { MESSAGE_LIST_ALL_KEYS }) => getPage(currentPage, MESSAGE_LIST_ALL_KEYS, 0),
    [MESSAGE_LIST_TOTAL_PAGES]: (s, { MESSAGE_LIST_ALL_KEYS }) => Math.ceil(MESSAGE_LIST_ALL_KEYS.length / PAGE_SIZE),
    [MESSAGE_LIST_UNREAD_FILTER_ENABLED]: ({ filter }) => filter === MessageListFilter.UNREAD
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
