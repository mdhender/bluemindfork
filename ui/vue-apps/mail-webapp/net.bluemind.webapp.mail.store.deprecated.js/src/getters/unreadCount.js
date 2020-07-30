export function unreadCount(state, rootState) {
    return folderUid => rootState.mail.folders[folderUid].unread;
}
