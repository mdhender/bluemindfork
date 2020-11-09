import ServiceLocator from "@bluemind/inject";

import { createOnlyMetadata } from "../../model/message";
import { CURRENT_MAILBOX } from "~getters";
import { SET_MESSAGE_LIST } from "~mutations";

const MAX_SEARCH_RESULTS = 500;
export const STATUS = {
    IDLE: "idle",
    LOADING: "loading",
    RESOLVED: "resolved",
    REJECTED: "rejected"
};

export const state = {
    status: STATUS.IDLE,
    pattern: null,
    searchFolder: null
};

export const mutations = {
    setPattern(state, pattern) {
        state.pattern = pattern;
    },
    setStatus(state, status) {
        state.status = status;
    },
    setSearchFolder(state, searchFolder) {
        state.searchFolder = searchFolder;
    }
};

export const actions = {
    search
};

export const getters = {
    isActive(state) {
        return state.status !== STATUS.IDLE;
    },
    isLoading(state) {
        return state.status === STATUS.LOADING;
    },
    isRejected: function (state) {
        return state.status === STATUS.REJECTED;
    },
    isResolved: function (state) {
        return state.status === STATUS.RESOLVED;
    }
};

export default {
    namespaced: true,
    state,
    mutations,
    actions,
    getters
};

async function search({ commit, dispatch, rootState, rootGetters }, { pattern, filter, folderKey }) {
    try {
        commit("setStatus", STATUS.LOADING);
        const mailboxUid = folderKey
            ? rootState.mail.folders[folderKey].mailboxRef.uid
            : rootGetters["mail/" + CURRENT_MAILBOX].key;
        const searchPayload = buildPayload(pattern, filter, folderKey ? folderKey : undefined);
        const searchResults = await ServiceLocator.getProvider("MailboxFoldersPersistence")
            .get(mailboxUid)
            .searchItems(searchPayload);
        if (!searchResults.results) searchResults.results = [];
        const messages = searchResults.results.map(res => {
            const underscoreIndex = res.containerUid.lastIndexOf("_");
            const offset = underscoreIndex >= 0 ? underscoreIndex + 1 : 0;
            const folderUid = res.containerUid.substring(offset);
            return createOnlyMetadata({ internalId: res.itemId, folder: { key: folderUid, uid: folderUid } });
        });
        commit("mail/" + SET_MESSAGE_LIST, messages, { root: true });
        const result = await dispatch(
            "mail-webapp/messages/multipleByKey",
            messages.slice(0, 40).map(m => m.key),
            { root: true }
        );
        commit("setStatus", STATUS.RESOLVED);
        return result;
    } catch (err) {
        commit("setStatus", STATUS.REJECTED);
    }
}

function buildPayload(pattern, filter, folderKey) {
    const flags = filter === "unread" ? "is:unread" : filter === "flagged" ? "is:flagged" : "";
    return {
        query: {
            searchSessionId: undefined,
            query: pattern,
            recordQuery: flags,
            maxResults: MAX_SEARCH_RESULTS,
            offset: undefined,
            scope: {
                folderScope: {
                    folderUid: folderKey
                },
                isDeepTraversal: false
            }
        },
        sort: {
            criteria: [
                {
                    field: "date",
                    order: "Desc"
                }
            ]
        }
    };
}
