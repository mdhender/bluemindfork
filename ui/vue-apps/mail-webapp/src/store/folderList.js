import Vue from "vue";
import {
    FOLDER_LIST_IS_FILTERED,
    FOLDER_LIST_IS_LOADING,
    FOLDER_LIST_LIMIT_FOR_MAILSHARE,
    FOLDER_LIST_LIMIT_FOR_USER
} from "~/getters";
import {
    RESET_FILTER_LIMITS,
    SET_FILTER_PATTERN,
    SET_FILTER_RESULTS,
    SET_FILTER_LIMIT,
    SET_FILTER_STATUS,
    TOGGLE_EDIT_FOLDER
} from "~/mutations";

const DEFAULT_LIMIT = 10;

export const FolderListStatus = {
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
        [RESET_FILTER_LIMITS]: state => {
            state.limits = {};
        },
        [SET_FILTER_PATTERN]: (state, pattern) => {
            state.pattern = pattern?.trim();
        },
        [SET_FILTER_RESULTS]: (state, results) => {
            state.results = results;
        },
        [SET_FILTER_LIMIT]: (state, { mailbox, limits }) => {
            Vue.set(state.limits, mailbox.key, limits);
        },
        [SET_FILTER_STATUS]: (state, status) => {
            state.status = status;
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
