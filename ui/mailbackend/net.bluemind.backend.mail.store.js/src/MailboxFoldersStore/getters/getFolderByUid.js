export function getFolderByUid(state) {
    return uid => state.items.find(item => item.uid == uid);
}
