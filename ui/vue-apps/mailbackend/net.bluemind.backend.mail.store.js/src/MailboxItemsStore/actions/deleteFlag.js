import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function deleteFlag({ commit, dispatch, state }, { messageKeys, mailboxItemFlag }) {
    const keysByFolder = ItemUri.urisByContainer(messageKeys);

    const promises = Object.keys(keysByFolder).map(folder =>
        deleteFlagByFolder(folder, keysByFolder[folder], mailboxItemFlag).catch(() =>
            dispatch(
                "multipleByKey",
                keysByFolder[folder].filter(key => state.items[key])
            ).then(() => {
                throw new Error();
            })
        )
    );

    return Promise.all(promises).then(() => commit("deleteFlag", { messageKeys, mailboxItemFlag }));
}

function deleteFlagByFolder(folderUid, keys, mailboxItemFlag) {
    const ids = keys.map(key => ItemUri.item(key));
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .deleteFlag({ itemsId: ids, mailboxItemFlag });
}
