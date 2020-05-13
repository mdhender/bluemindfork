export function unreadCount(state) {
    return folderUid => {
        return (state.foldersData[folderUid] && state.foldersData[folderUid].unread) || 0;
    };
}
