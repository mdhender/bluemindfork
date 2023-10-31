import {
    ADD_SELECTED_FILE,
    INSERT_AS_ATTACHMENT,
    INSERT_AS_LINK,
    REMOVE_SELECTED_FILE,
    RESET_PATH,
    RESET_SELECTED_FILES,
    SET_PATH,
    SET_SEARCH_PATTERN,
    SET_SELECTED_FILES,
    UNSET_SEARCH_PATTERN,
    SET_SEARCH_MODE,
    UNSET_SEARCH_MODE
} from "./mutations";
import { HAS_VALID_PATTERN } from "./getters";
import { RESET_CHOOSER } from "./actions";

const ROOT_PATH = "/";

export const chooserStore = {
    namespaced: true,
    state: {
        rootPath: ROOT_PATH,
        path: ROOT_PATH,
        pattern: null,
        selectedFiles: [],
        isSearchMode: false,
        insertAsLink: true
    },
    mutations: {
        [RESET_PATH](state) {
            state.path = ROOT_PATH;
        },
        [SET_PATH](state, path) {
            state.path = path;
        },
        [SET_SEARCH_PATTERN](state, pattern) {
            state.pattern = pattern;
        },
        [UNSET_SEARCH_PATTERN](state) {
            state.pattern = null;
        },
        [ADD_SELECTED_FILE](state, file) {
            state.selectedFiles.push(file);
        },
        [REMOVE_SELECTED_FILE](state, file) {
            const index = state.selectedFiles.findIndex(({ path }) => path === file.path);
            state.selectedFiles.splice(index, 1);
        },
        [RESET_SELECTED_FILES](state) {
            state.selectedFiles = [];
        },
        [SET_SELECTED_FILES](state, files) {
            state.selectedFiles = [...files];
        },
        [SET_SEARCH_MODE](state) {
            state.isSearchMode = true;
        },
        [UNSET_SEARCH_MODE](state) {
            state.isSearchMode = false;
        },
        [INSERT_AS_ATTACHMENT](state) {
            state.insertAsLink = false;
        },
        [INSERT_AS_LINK](state) {
            state.insertAsLink = true;
        }
    },
    getters: {
        [HAS_VALID_PATTERN](state) {
            return state.pattern !== null;
        }
    },
    actions: {
        [RESET_CHOOSER]({ commit }) {
            commit(RESET_SELECTED_FILES);
            commit(RESET_PATH);
            commit(UNSET_SEARCH_MODE);
            commit(UNSET_SEARCH_PATTERN);
        }
    }
};
