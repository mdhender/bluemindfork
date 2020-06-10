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
    isActive(state) {
        return state.status !== STATUS.IDLE;
    },
    isLoading(state) {
        return state.status === STATUS.LOADING;
    },
    isRejected: function(state) {
        return state.status === STATUS.REJECTED;
    },
    isResolved: function(state) {
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

async function search({ commit, dispatch, rootGetters, rootState }, { pattern, filter }) {
    try {
        commit("setStatus", STATUS.LOADING);
        const folderUid = ItemUri.item(rootState["mail-webapp"].currentFolderKey);
        const mailboxUid = rootGetters["mail-webapp/currentMailbox"].mailboxUid;
        commit("setPattern", pattern);
        const searchPayload = buildPayload(pattern, filter, folderUid);
        const searchResults = await ServiceLocator.getProvider("MailboxFoldersPersistence")
            .get(mailboxUid)
            .searchItems(searchPayload);
        const itemKeys = searchResults.results.map(message => message.itemId).map(id => ItemUri.encode(id, folderUid));
        commit("mail-webapp/messages/setItemKeys", itemKeys, { root: true });
        const result = await dispatch("mail-webapp/messages/multipleByKey", itemKeys.slice(0, 40), { root: true });
        commit("setStatus", STATUS.RESOLVED);
        return result;
    } catch (err) {
        console.error(err);
        commit("setStatus", STATUS.REJECTED);
    }
}

function buildPayload(pattern, filter, folderUid) {
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
