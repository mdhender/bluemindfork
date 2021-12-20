import Vue from "vue";
import { asynchronous } from "../utils/asynchronous";
import {
    MAILBOXES,
    MAILBOX_FOLDERS,
    FOLDER_LIST_IS_EMPTY,
    FOLDER_LIST_IS_FILTERED,
    FOLDER_LIST_IS_LOADING,
    FOLDER_LIST_RESULTS
} from "~/getters";
import {
    RESET_FILTER_LIMITS,
    SET_FILTER_PATTERN,
    SET_FILTER_RESULTS,
    SET_FILTER_LIMIT,
    SET_FILTER_STATUS,
    TOGGLE_EDIT_FOLDER
} from "~/mutations";
import { FILTER_FOLDERS } from "~/actions";

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
        [FOLDER_LIST_IS_EMPTY]: state => {
            for (let key in state.results) {
                if (state.results[key].length > 0) {
                    return false;
                }
            }
            return true;
        },
        [FOLDER_LIST_IS_FILTERED]: state => {
            return state.pattern && state.pattern !== "";
        },
        [FOLDER_LIST_IS_LOADING]: state => {
            return state.status === FolderListStatus.LOADING;
        },
        [FOLDER_LIST_RESULTS]: state => mailbox => {
            return state.results[mailbox.key];
        }
    },
    actions: {
        [FILTER_FOLDERS]: ({ state, getters, commit }) => {
            if (state.pattern !== "") {
                commit(SET_FILTER_STATUS, FolderListStatus.LOADING);
                return asynchronous(() => {
                    const results = {};
                    getters[`mail/${MAILBOXES}`].forEach(mailbox => {
                        const limits = state.limits[mailbox.key] || DEFAULT_LIMIT;
                        const folders = getters[`mail/${MAILBOX_FOLDERS}`](mailbox);
                        results[mailbox.key] = [];
                        for (var i = 0, folder = folders[i]; i < folders.length - 1 && results.length < limits; i++) {
                            if (
                                folder.path.toLowerCase().includes(state.pattern.toLowerCase()) ||
                                folder.name.toLowerCase().includes(state.pattern.toLowerCase())
                            ) {
                                results[mailbox.key].push(folder.key);
                            }
                        }
                    });
                    commit(SET_FILTER_RESULTS, results);
                    commit(SET_FILTER_STATUS, FolderListStatus.IDLE);
                });
            }
        }
    }
};
