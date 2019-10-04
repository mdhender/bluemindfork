// TODO Command pattern // Handle errors and undo
export function markAsRead({ dispatch, state, commit }, messageId) {
    const folderUid = state.currentFolderUid;
    return dispatch("$_getIfNotPresent", { folder: folderUid, id: messageId }).then(message => {
        if (message.states.includes("not-seen")) {
            commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            return dispatch("messages/updateSeen", { folder: folderUid, id: messageId, isSeen: true });
        }
    });
    //FIXME:
    // .catch(() => {});
    // .then(() => refresh mail list if there is a unread filter);
}
