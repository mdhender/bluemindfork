import ServiceLocator from "@bluemind/inject";

export function all({ commit }) {
    return ServiceLocator.getProvider("MailboxFoldersPersistance")
        .get()
        .all()
        .then(items => {
            commit("storeItems", items);
        });
}
