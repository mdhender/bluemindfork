import ServiceLocator from "@bluemind/inject";

export function getCompleteById({ commit }, { folder, id }) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folder)
        .getCompleteById(id)
        .then(item => commit("storeItems", [item]));
}
