import ServiceLocator from "@bluemind/inject";

export function all({ commit }) {
    return ServiceLocator.getProvider("MailboxFoldersPersistence")
        .get()
        .all()
        .then(items => {
            commit("storeItems", items);
        });
}
