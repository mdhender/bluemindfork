//FIXME: Search store should be refactored
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

const MAX_SEARCH_RESULTS = 500;

export function search({ state, commit, dispatch, getters }, { pattern, filter }) {
    commit("messages/clearItems");
    commit("clearCurrentMessage");

    // FIXME state.currentFolderKey should not be undefined/null at this point
    const folderUid = state.currentFolderKey ? ItemUri.item(state.currentFolderKey) : "";

    if (state.messageFilter !== filter) {
        commit("setMessageFilter", filter);
    }
    // TODO complete this once ES stuff had been fixed on core side
    const excludedFlagsESPattern = filter === "unread" ? " " : "";

    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(getters.currentMailbox.mailboxUid)
        .searchItems({
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
