import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function addFlag({ state, commit, dispatch }, { messageKeys, mailboxItemFlag }) {
    const keysByFolder = ItemUri.urisByContainer(messageKeys);

    const promises = Object.keys(keysByFolder).map(folder =>
        addFlagByFolder(folder, keysByFolder[folder], mailboxItemFlag).catch(() =>
            dispatch(
                "multipleByKey",
                keysByFolder[folder].filter(key => state.items[key])
            ).then(() => {
                throw new Error();
            })
        )
    );

    return Promise.all(promises).then(() => commit("addFlag", { messageKeys, mailboxItemFlag }));
}

function addFlagByFolder(folderUid, keys, mailboxItemFlag) {
    const ids = keys.map(key => ItemUri.item(key));
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .addFlag({ itemsId: ids, mailboxItemFlag });
}
