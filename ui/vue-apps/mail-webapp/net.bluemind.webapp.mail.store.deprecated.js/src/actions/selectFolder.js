import ItemUri from "@bluemind/item-uri";

export function selectFolder({ commit, dispatch, state }, folder) {
    const encodedKey = ItemUri.encode(folder.key, folder.mailbox);
    if (state.currentFolderKey !== encodedKey) {
        commit("setCurrentFolder", encodedKey);
    }
    return dispatch("loadUnreadCount", folder.key);
}
