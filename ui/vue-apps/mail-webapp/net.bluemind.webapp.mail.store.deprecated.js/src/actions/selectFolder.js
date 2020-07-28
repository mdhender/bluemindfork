import ItemUri from "@bluemind/item-uri";

export function selectFolder({ commit, dispatch, state }, folderKey) {
    const folderUid = ItemUri.item(folderKey);
    if (state.currentFolderKey !== folderKey) {
        commit("setCurrentFolder", folderKey);
    }
    return dispatch("loadUnreadCount", folderUid);
}
