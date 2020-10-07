import mutationTypes from "./mutationTypes";

const state = {
    pattern: null,
    folder: null
};

const mutations = {
    [mutationTypes.SET_SEARCH_PATTERN](state, pattern) {
        state.pattern = pattern;
    },
    [mutationTypes.SET_SEARCH_FOLDER](state, folder) {
        state.folder = folder;
    }
};

const getters = {
    MESSAGE_LIST_IS_SEARCH_MODE: ({ pattern }) => pattern && pattern.trim().length > 0
};

export default { state, mutations, getters };
