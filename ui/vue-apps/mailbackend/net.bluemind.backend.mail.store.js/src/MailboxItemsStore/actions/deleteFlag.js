import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function deleteFlag({ commit }, { messageKey, mailboxItemFlag }) {
    const [id, folder] = ItemUri.decode(messageKey);
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folder);
    return service
        .deleteFlag([{ itemsId: [id], mailboxItemFlag }])
        .then(() => commit("deleteFlag", { messageKey, mailboxItemFlag }));
}
