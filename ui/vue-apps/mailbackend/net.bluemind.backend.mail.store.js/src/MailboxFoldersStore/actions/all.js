import ServiceLocator from "@bluemind/inject";

export function all({ commit }, mailboxUid) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get(mailboxUid)
        .all()
        .then(items => {
            items = items.filter(item => !item.value.deleted);
            commit("storeItems", { items, mailboxUid });
        });
}
