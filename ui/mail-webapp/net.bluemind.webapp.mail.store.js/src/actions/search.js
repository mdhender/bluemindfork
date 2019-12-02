//FIXME: Search store should be refactored
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

const MAX_SEARCH_RESULTS = 500;

export function search({ state, commit, dispatch, getters }, pattern) {
    commit("messages/clearItems");
    commit("clearCurrentMessage");
    const folderUid = ItemUri.item(state.currentFolderKey);
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(getters.my.mailboxUid)
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
            commit("messages/setItemKeys", { ids, folderUid });
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", false);
            return dispatch("messages/multipleByKey", state.messages.itemKeys.slice(0, 200));
        })
        .catch(() => {
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", true);
        });
}
