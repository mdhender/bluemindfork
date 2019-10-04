export function currentFolder(state) {
    return state.folders.items.find(item => item.uid == state.currentFolderUid);
}
