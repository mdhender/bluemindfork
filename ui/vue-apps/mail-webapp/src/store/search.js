import { IS_SEARCH_ENABLED, HAS_PATTERN, IS_TYPING_IN_SEARCH } from "~/getters";
import {
    RESET_CURRENT_SEARCH_PATTERN,
    SET_CURRENT_SEARCH_DEEP,
    SET_CURRENT_SEARCH_FOLDER,
    SET_CURRENT_SEARCH_PATTERN,
    SET_HAS_MORE_RESULTS,
    SET_SEARCH_QUERY_DEEP,
    SET_SEARCH_QUERY_FOLDER,
    SET_SEARCH_QUERY_PATTERN
} from "~/mutations";
import { RESET_CURRENT_SEARCH } from "~/actions";

const state = {
    searchQuery: {
        pattern: null,
        folder: null,
        deep: true
    },
    currentSearch: {
        pattern: null,
        folder: null,
        deep: true
    },
    hasMoreResults: false
};

const mutations = {
    [SET_SEARCH_QUERY_PATTERN](state, pattern) {
        state.searchQuery.pattern = pattern;
        state.currentSearch.pattern = pattern;
    },
    [SET_SEARCH_QUERY_FOLDER](state, folder) {
        state.searchQuery.folder = folder;
        state.currentSearch.folder = folder;
    },
    [SET_SEARCH_QUERY_DEEP](state, deep) {
        state.searchQuery.deep = deep;
        state.currentSearch.deep = deep;
    },
    [SET_CURRENT_SEARCH_PATTERN](state, pattern) {
        state.currentSearch.pattern = pattern?.trim();
    },
    [SET_CURRENT_SEARCH_FOLDER](state, folder) {
        state.currentSearch.folder = folder;
    },
    [SET_CURRENT_SEARCH_DEEP](state, deep) {
        state.currentSearch.deep = deep;
    },
    [SET_HAS_MORE_RESULTS](state, hasMoreResults) {
        state.hasMoreResults = hasMoreResults;
    },
    [RESET_CURRENT_SEARCH_PATTERN](state) {
        state.currentSearch.pattern = null;
    }
};

const actions = {
    [RESET_CURRENT_SEARCH]({ commit }) {
        commit(RESET_CURRENT_SEARCH_PATTERN);
        commit(SET_CURRENT_SEARCH_DEEP, true);
    }
};

const getters = {
    [IS_SEARCH_ENABLED]: ({ searchQuery }) => !!searchQuery.pattern && searchQuery.pattern.trim().length > 0,
    [HAS_PATTERN]: ({ currentSearch }) => currentSearch.pattern?.length >= 0,
    [IS_TYPING_IN_SEARCH]: ({ currentSearch, searchQuery }) =>
        currentSearch.pattern !== null && currentSearch.pattern !== searchQuery.pattern
};

export default { state, actions, mutations, getters };
