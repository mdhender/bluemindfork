import ServiceLocator from "@bluemind/inject";

export function sortedIds({ commit }, { sorted, folderUid }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid);
    return service.sortedIds(sorted).then(ids => {
        commit("setSortedIds", ids);
    });
}
