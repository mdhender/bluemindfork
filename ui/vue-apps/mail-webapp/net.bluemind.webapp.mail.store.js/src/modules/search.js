import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

const MAX_SEARCH_RESULTS = 500;
export const STATUS = {
    IDLE: "idle",
    LOADING: "loading",
    RESOLVED: "resolved",
    REJECTED: "rejected"
};

export const state = {
    status: STATUS.IDLE,
    pattern: null
};

export const mutations = {
    setPattern(state, pattern) {
        state.pattern = pattern;
    },
    setStatus(state, status) {
        state.status = status;
    }
};

export const actions = {
    search
};

export const getters = {
    isLoading(state) {
        return state.status === STATUS.LOADING;
    },
    isError(state) {
        return state.status === STATUS.REJECTED;
    }
};

export default {
    namespaced: true,
    state,
    mutations,
    actions,
    getters
};

async function search({ commit, dispatch, rootGetters, rootState }, { pattern, filter }) {
    try {
        clearSomeUnrelatedState({ commit });
        updateUnrelatedFilter({ commit }, filter);
        const folderUid = getterForFolderUidThatShouldBeGivenToTheAction(rootState);
        const mailboxUid = rootGetters["mail-webapp/currentMailbox"].mailboxUid;
        return doTheSearch({ commit, dispatch }, { pattern, filter, folderUid, mailboxUid });
    } catch (err) {
        console.error(err);
        commit("setStatus", STATUS.REJECTED);
    }
}

function getterForFolderUidThatShouldBeGivenToTheAction(rootState) {
    return rootState["mail-webapp"].currentFolderKey && ItemUri.item(rootState["mail-webapp"].currentFolderKey); //FIXME: state.currentFolderKey should not be undefined/null at this point
}

async function doTheSearch({ commit, dispatch }, { pattern, filter, folderUid, mailboxUid }) {
    try {
        commit("setStatus", STATUS.LOADING);
        commit("setPattern", pattern);
        const searchPayload = buildPayload(pattern, filter, folderUid);
        const searchResults = await searchItems(mailboxUid, searchPayload);
        const itemKeys = toItemKeys(searchResults, folderUid);
        commit("mail-webapp/messages/setItemKeys", itemKeys, { root: true });
        const result = await dispatch("mail-webapp/messages/multipleByKey", itemKeys.slice(0, 200), { root: true });
        commit("setStatus", STATUS.RESOLVED);
        return result;
    } catch (err) {
        console.error(err);
        commit("setStatus", STATUS.REJECTED);
    }
}

async function searchItems(mailboxUid, searchPayload) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(mailboxUid)
        .searchItems(searchPayload);
}

function updateUnrelatedFilter({ commit }, filter) {
    commit("mail-webapp/setMessageFilter", filter, { root: true });
}

function clearSomeUnrelatedState({ commit }) {
    commit("mail-webapp/messages/clearItems", { root: true });
    commit("mail-webapp/currentMessage/clear", { root: true });
    commit("mail-webapp/messages/clearParts", { root: true });
}

function toItemKeys(searchResults, folderUid) {
    return searchResults.results.map(message => message.itemId).map(id => ItemUri.encode(id, folderUid));
}

function buildPayload(pattern, filter, folderUid) {
    const excludedFlagsESPattern = filter === "unread" ? " " : ""; //FIXME: complete this once ES stuff had been fixed on core side
    return {
        query: {
            searchSessionId: undefined,
            query: pattern + excludedFlagsESPattern,
            maxResults: MAX_SEARCH_RESULTS,
            offset: undefined,
            scope: {
                folderScope: {
                    folderUid
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
