import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function multipleByKey({ commit }, messageKeys) {
    if (messageKeys.length === 0) return Promise.resolve();
    const messagesByFolder = {};
    messageKeys.forEach(key => {
        const [id, folder] = ItemUri.decode(key);
        if (!messagesByFolder[folder]) messagesByFolder[folder] = [];
        messagesByFolder[folder].push(id);
    });
    const folders = Object.keys(messagesByFolder);
    return Promise.all(folders.map(folderUid => getFolderMessages(folderUid, messagesByFolder[folderUid], commit)));
}

function getFolderMessages(folderUid, messages, commit) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .multipleById(messages)
        .then(items => commit("storeItems", { items, folderUid }));
}
