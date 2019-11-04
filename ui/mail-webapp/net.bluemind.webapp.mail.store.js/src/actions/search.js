//FIXME: Search store should be refactored
import ServiceLocator from "@bluemind/inject";

const MAX_SEARCH_RESULTS = 500;

export function search({ commit, dispatch }, { folderUid, pattern }) {
    commit("messages/clearItems");
    commit("clearCurrentMessage");
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get()
        .searchItems({
            query: {
                searchSessionId: undefined,
                query: pattern,
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
        })
        .then(searchResults => {
            const ids = searchResults.results.map(message => message.itemId);
            commit("messages/setSortedIds", ids);
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", false);
            return dispatch("messages/multipleById", { folderUid, ids: ids.slice(0, 200) });
        })
        .catch(() => {
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", true);
        });
}
