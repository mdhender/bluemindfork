import { CONVERSATION_LIST_IS_SEARCH_MODE } from "~/getters";
import { SET_SEARCH_FOLDER, SET_SEARCH_PATTERN } from "~/mutations";

const state = {
    pattern: null,
    folder: null
};

const mutations = {
    [SET_SEARCH_PATTERN](state, pattern) {
        state.pattern = pattern;
    },
    [SET_SEARCH_FOLDER](state, folder) {
        state.folder = folder;
    }
};

const getters = {
    [CONVERSATION_LIST_IS_SEARCH_MODE]: ({ pattern }) => pattern && pattern.trim().length > 0
};

export default { state, mutations, getters };
