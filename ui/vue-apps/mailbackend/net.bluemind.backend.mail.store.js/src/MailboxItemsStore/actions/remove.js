import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function remove({ commit }, messageKeys) {
    messageKeys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    return Promise.all(
        Object.keys(messageKeysByFolder).map(folder => removeByFolder(commit, messageKeysByFolder[folder], folder))
    );
}

function removeByFolder(commit, messageKeys, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .multipleDeleteById(messageKeys.map(key => ItemUri.item(key)))
        .then(() => commit("removeItems", messageKeys));
}
