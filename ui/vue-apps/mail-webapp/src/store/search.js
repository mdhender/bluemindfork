import { CONVERSATION_LIST_IS_FILTERED } from "~/getters";
import { SET_SEARCH_MODE, SET_SEARCH_FOLDER, SET_SEARCH_DEEP, SET_SEARCH_PATTERN } from "~/mutations";

const state = {
    pattern: null,
    folder: null,
    deep: false,
    searchMode: false
};

const mutations = {
    [SET_SEARCH_PATTERN](state, pattern) {
        state.pattern = pattern;
    },
    [SET_SEARCH_FOLDER](state, folder) {
        state.folder = folder;
    },
    [SET_SEARCH_DEEP](state, deep) {
        state.deep = deep;
    },
    [SET_SEARCH_MODE](state, value) {
        state.searchMode = value;
    }
};

const getters = {
    [CONVERSATION_LIST_IS_FILTERED]: ({ pattern }) => !!pattern && pattern.trim().length > 0
};

export default { state, mutations, getters };
