import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function updateSeen({ commit }, { messageKey, isSeen }) {
    const [id, folder] = ItemUri.decode(messageKey);
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folder);
    return service
        .updateSeens([{ itemId: id, seen: isSeen, mdnSent: false }])
        .then(() => commit("updateSeen", { messageKey, isSeen }));
}
