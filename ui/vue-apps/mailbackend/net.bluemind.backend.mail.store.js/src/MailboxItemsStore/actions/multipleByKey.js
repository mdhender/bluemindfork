import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export async function multipleByKey({ commit }, messageKeys) {
    const idsByFolderUid = getIdsByFolderUid(messageKeys);
    const promises = Object.entries(idsByFolderUid).map(async ([folderUid, ids]) => {
        const items = await getMessages(folderUid, ids);
        commit("storeItems", {
            folderUid,
            items
        });
    });
    const multipleByKeyResults = await Promise.all(promises);
    return multipleByKeyResults;
}

function getIdsByFolderUid(messageKeys) {
    return messageKeys
        .map(key => ItemUri.decode(key))
        .reduce((items, [item, key]) => ({ ...items, [key]: items[key] ? [...items[key], item] : [item] }), {});
}

function getMessages(folderUid, ids) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .multipleById(ids);
}
