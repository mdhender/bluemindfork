//FIXME: A action which return something ?
import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function create({ commit, getters }, { name, parentUid, mailboxUid }) {
    const service = ServiceLocator.getProvider("MailboxFoldersPersistence").get(mailboxUid);
    let fullName = name;
    if (parentUid !== null) {
        const parent = getters.getFolderByKey(ItemUri.encode(parentUid, mailboxUid));
        fullName = parent.value.fullName + "/" + name;
    }
    return service
        .createBasic({ name, fullName, parentUid, deleted: false })
        .then(({ uid }) => service.getComplete(uid))
        .then(mailboxFolder => {
            commit("storeItems", { items: [mailboxFolder], mailboxUid });
            return ItemUri.encode(mailboxFolder.uid, mailboxUid);
        });
}
