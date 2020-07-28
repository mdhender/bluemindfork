import ItemUri from "@bluemind/item-uri";

export async function $_Vuebus_mailboxRecordsChanged({ dispatch, state }) {
    if (!state.currentFolderKey) {
        console.error("this action must not be called if no currentFolderKey is set.");
        return;
    }

    const currentFolderUid = ItemUri.item(state.currentFolderKey);

    await dispatch("messages/list", { sorted: state.sorted, folderUid: currentFolderUid, filter: state.messageFilter });

    const numberOfMessagesWithMetadata = Object.entries(state.messages.items).length;
    const messagesToFetch = state.messages.itemKeys.slice(0, Math.min(numberOfMessagesWithMetadata, 200));
    dispatch("messages/multipleByKey", messagesToFetch);
    dispatch("loadUnreadCount", currentFolderUid);
}
