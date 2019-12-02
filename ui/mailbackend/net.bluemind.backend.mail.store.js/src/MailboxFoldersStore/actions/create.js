//FIXME: A action which return something ?
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function create({ commit }, { name, parentUid, mailboxUid }) {
    const service = ServiceLocator.getProvider("MailboxFoldersPersistence").get(mailboxUid);
    return service
        .createBasic({
            name: name,
            fullName: name,
            parentUid: parentUid,
            deleted: false
        })
        .then(({ uid }) => {
            return service.getComplete(uid);
        })
        .then(mailboxFolder => {
            commit("storeItems", { items: [mailboxFolder], mailboxUid });
            return ItemUri.encode(mailboxFolder.uid, mailboxUid);
        });
}
