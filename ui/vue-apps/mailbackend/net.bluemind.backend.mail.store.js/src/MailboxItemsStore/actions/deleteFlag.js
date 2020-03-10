import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function deleteFlag({ commit }, { messageKeys, mailboxItemFlag }) {
    const idsByFolder = ItemUri.itemsByContainer(messageKeys);

    const promises = Object.keys(idsByFolder).map(folder =>
        deleteFlagByFolder(folder, idsByFolder[folder], mailboxItemFlag)
    );

    return Promise.all(promises).then(() => commit("deleteFlag", { messageKeys, mailboxItemFlag }));
}

function deleteFlagByFolder(folderUid, ids, mailboxItemFlag) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .deleteFlag({ itemsId: ids, mailboxItemFlag });
}
