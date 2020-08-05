export function unreadCount(state, getters, rootState) {
    return folderUid => rootState.mail.folders[folderUid].unread;
}
