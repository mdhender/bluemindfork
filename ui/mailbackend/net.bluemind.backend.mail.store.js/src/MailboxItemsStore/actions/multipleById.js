import ServiceLocator from "@bluemind/inject";

export function multipleById({ commit }, { folderUid, ids }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistence").get(folderUid);
    return service.multipleById(ids).then(items => commit("storeItems", items));
}
