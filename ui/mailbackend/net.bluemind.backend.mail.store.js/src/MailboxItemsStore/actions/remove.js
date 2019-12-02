import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function remove({ commit }, messageKey) {
    const [messageId, folderUid] = ItemUri.decode(messageKey);

    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .deleteById(messageId)
        .then(() => commit("removeItems", [messageKey]));
}
