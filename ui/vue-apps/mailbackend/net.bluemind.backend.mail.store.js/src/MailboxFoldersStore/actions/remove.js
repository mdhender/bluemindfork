import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export async function remove({ getters, commit }, folderKey) {
    const mailboxUid = ItemUri.container(folderKey);
    const folder = getters.getFolderByKey(folderKey);
    commit("removeItems", [folderKey]);
    try {
        const service = injector.getProvider("MailboxFoldersPersistence").get(mailboxUid);
        await service.deepDelete(folder.internalId);
    } catch (e) {
        commit("storeItems", { items: [folder], mailboxUid });
        throw e;
    }
}
