import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function multipleByKey({ commit }, messageKeys) {
    const idsByFolder = ItemUri.itemsByContainer(messageKeys);
    return Promise.all(
        Object.keys(idsByFolder).map(folderUid => getMessages(folderUid, idsByFolder[folderUid], commit))
    );
}

function getMessages(folderUid, ids, commit) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .multipleById(ids)
        .then(items => commit("storeItems", { items, folderUid }));
}
