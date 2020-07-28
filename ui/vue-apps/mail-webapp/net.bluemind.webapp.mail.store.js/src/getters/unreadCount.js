export function unreadCount(state) {
    return folderUid => (state.foldersData[folderUid] && state.foldersData[folderUid].unread) || 0;
}
