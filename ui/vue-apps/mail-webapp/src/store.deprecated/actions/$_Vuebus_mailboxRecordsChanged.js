export async function $_Vuebus_mailboxRecordsChanged({ dispatch, state, rootState, getters }) {
    const activeFolderKey = rootState.mail.activeFolder;
    if (!activeFolderKey) {
        console.error("this action must not be called if no activeFolderKey is set.");
        return;
    }

    await dispatch("messages/list", { folderUid: activeFolderKey, filter: state.messageFilter });

    const numberOfMessagesWithMetadata = getters["messages/messages"].length;
    const messagesToFetch = rootState.mail.messageList.messageKeys.slice(
        0,
        Math.min(numberOfMessagesWithMetadata, 200)
    );

    dispatch("messages/multipleByKey", messagesToFetch);
    dispatch("loadUnreadCount", activeFolderKey);
}
