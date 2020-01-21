export function getFolderByPath(state, getters) {
    return (path, mailboxUid) => get(getters.getFoldersByMailbox(mailboxUid), path.split("/").filter(Boolean), null);
}

function get(folders, hierarchy, parentUid) {
    const name = hierarchy.shift();
    const folder = folders.find(folder => folder.value.parentUid === parentUid && name === folder.value.name);
    if (!folder) {
        return null;
    }
    if (hierarchy.length > 0) {
        return get(folders, hierarchy, folder.uid);
    }
    return folder;
}
