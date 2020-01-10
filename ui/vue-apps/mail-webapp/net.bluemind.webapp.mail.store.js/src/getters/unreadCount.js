export function unreadCount(state) {
    return folderKey => {
        return (state.foldersData[folderKey] && state.foldersData[folderKey].unread) || 0;
    };
}
