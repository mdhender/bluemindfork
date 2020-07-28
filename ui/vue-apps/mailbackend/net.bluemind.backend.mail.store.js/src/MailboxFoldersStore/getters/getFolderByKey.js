import ItemUri from "@bluemind/item-uri";

export function getFolderByKey(state, getters, rootState) {
    const foldersKeys = Object.values(rootState.mail.folders).map(folder => ItemUri.encode(folder.uid, folder.mailbox));
    return key => getters.folders[foldersKeys.indexOf(key)];
}
