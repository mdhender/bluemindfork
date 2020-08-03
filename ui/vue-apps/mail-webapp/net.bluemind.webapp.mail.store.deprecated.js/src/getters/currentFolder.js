import ItemUri from "@bluemind/item-uri";

export function currentFolder(state, getters, rootState) {
    return rootState.mail.folders[ItemUri.item(state.currentFolderKey)];
}
