import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function addFlag({ commit }, { messageKey, mailboxItemFlag }) {
    const [id, folder] = ItemUri.decode(messageKey);
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folder);
    return service
        .addFlag({ itemsId: [id], mailboxItemFlag })
        .then(() => commit("addFlag", { messageKey, mailboxItemFlag }));
}
