import Vue from "vue";
import {
    FOLDER_LIST_IS_FILTERED,
    FOLDER_LIST_IS_LOADING,
    FOLDER_LIST_LIMIT_FOR_MAILSHARE,
    FOLDER_LIST_LIMIT_FOR_USER
} from "~/getters";
import {
    RESET_FOLDER_FILTER_LIMITS,
    SET_FOLDER_FILTER_PATTERN,
    SET_FOLDER_FILTER_RESULTS,
    SET_FOLDER_FILTER_LIMIT,
    SET_FOLDER_FILTER_LOADED,
    SET_FOLDER_FILTER_LOADING,
    TOGGLE_EDIT_FOLDER
} from "~/mutations";

const DEFAULT_LIMIT = 10;

const FolderListStatus = {
    IDLE: Symbol("idle"),
    LOADING: Symbol("loading")
};

export default {
    state: {
        editing: undefined,
        pattern: "",
        results: {},
        limits: {},
        status: FolderListStatus.IDLE
    },
    mutations: {
        [RESET_FOLDER_FILTER_LIMITS]: state => {
            state.limits = {};
        },
        [SET_FOLDER_FILTER_PATTERN]: (state, pattern) => {
            state.pattern = pattern?.trim();
        },
        [SET_FOLDER_FILTER_RESULTS]: (state, results) => {
            state.results = results;
        },
        [SET_FOLDER_FILTER_LIMIT]: (state, { mailbox, limits }) => {
            Vue.set(state.limits, mailbox.key, limits);
        },
        [SET_FOLDER_FILTER_LOADING]: state => {
            state.status = FolderListStatus.LOADING;
        },
        [SET_FOLDER_FILTER_LOADED]: state => {
            state.status = FolderListStatus.IDLE;
        },
        [TOGGLE_EDIT_FOLDER]: (state, key) => {
            if (state.editing && state.editing === key) {
                state.editing = undefined;
            } else {
                state.editing = key;
            }
        }
    },
    getters: {
        [FOLDER_LIST_IS_FILTERED]: state => {
            return Boolean(state.pattern);
        },
        [FOLDER_LIST_IS_LOADING]: state => {
            return state.status === FolderListStatus.LOADING;
        },
        [FOLDER_LIST_LIMIT_FOR_MAILSHARE]: state => state.limits.mailshares || DEFAULT_LIMIT,
        [FOLDER_LIST_LIMIT_FOR_USER]: state => ({ key }) => state.limits[key] || DEFAULT_LIMIT
    }
};
