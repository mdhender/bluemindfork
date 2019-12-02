import ServiceLocator from "@bluemind/inject";

export function all({ commit }, mailboxUid) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(mailboxUid)
        .all()
        .then(items => commit("storeItems", { items, mailboxUid }));
}
