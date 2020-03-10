import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function addFlag({ commit }, { messageKeys, mailboxItemFlag }) {
    const idsByFolder = ItemUri.itemsByContainer(messageKeys);

    const promises = Object.keys(idsByFolder).map(folder =>
        addFlagByFolder(folder, idsByFolder[folder], mailboxItemFlag)
    );

    return Promise.all(promises).then(() => commit("addFlag", { messageKeys, mailboxItemFlag }));
}

function addFlagByFolder(folderUid, ids, mailboxItemFlag) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .addFlag({ itemsId: ids, mailboxItemFlag });
}
