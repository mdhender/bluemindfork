import { CONVERSATION_LIST_IS_FILTERED } from "~/getters";
import { SET_SEARCH_MODE_MOBILE, SET_SEARCH_FOLDER, SET_SEARCH_PATTERN } from "~/mutations";

const state = {
    pattern: null,
    folder: null,
    searchModeMobile: false
};

const mutations = {
    [SET_SEARCH_PATTERN](state, pattern) {
        state.pattern = pattern;
    },
    [SET_SEARCH_FOLDER](state, folder) {
        state.folder = folder;
    },
    [SET_SEARCH_MODE_MOBILE](state, value) {
        state.searchModeMobile = value;
    }
};

const getters = {
    [CONVERSATION_LIST_IS_FILTERED]: ({ pattern }) => pattern && pattern.trim().length > 0
};

export default { state, mutations, getters };
