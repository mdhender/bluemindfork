import ServiceLocator from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function multipleByKey({ commit }, messageKeys) {
    if (messageKeys.length == 0) return Promise.resolve();
    const messagesByFolder = {};
    messageKeys.forEach(key => {
        const [id, folder] = ItemUri.decode(key);
        if (!messagesByFolder[folder]) messagesByFolder[folder] = [];
        messagesByFolder[folder].push(id);
    });
    const serviceProvider = ServiceLocator.getProvider("MailboxItemsPersistence");
    return Promise.all(
        Object.keys(messagesByFolder).map(folderUid =>
            serviceProvider
                .get(folderUid)
                .multipleById(messagesByFolder[folderUid])
                .then(items => commit("storeItems", { items, folderUid }))
        )
    );
}
