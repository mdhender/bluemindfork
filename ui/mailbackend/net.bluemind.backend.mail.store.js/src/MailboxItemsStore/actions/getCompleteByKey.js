import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function getCompleteByKey({ commit }, messageKey) {
    const [id, folderUid] = ItemUri.decode(messageKey);
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .getCompleteById(id)
        .then(item => commit("storeItems", { items: [item], folderUid }));
}
