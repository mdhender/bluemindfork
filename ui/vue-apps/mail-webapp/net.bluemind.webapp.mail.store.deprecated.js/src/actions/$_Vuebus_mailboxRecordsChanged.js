export async function $_Vuebus_mailboxRecordsChanged({ dispatch, state, rootState }) {
    const activeFolderKey = rootState.mail.activeFolder;
    if (!activeFolderKey) {
        console.error("this action must not be called if no activeFolderKey is set.");
        return;
    }

    await dispatch("messages/list", { sorted: state.sorted, folderUid: activeFolderKey, filter: state.messageFilter });

    const numberOfMessagesWithMetadata = Object.entries(state.messages.items).length;
    const messagesToFetch = state.messages.itemKeys.slice(0, Math.min(numberOfMessagesWithMetadata, 200));
    dispatch("messages/multipleByKey", messagesToFetch);
    dispatch("loadUnreadCount", activeFolderKey);
}
