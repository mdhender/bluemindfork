import Folder from "./Folder";

export function tree(state) {
    const nodeMap = new Map();
    state.folders.forEach(folderItem => {
        const folder = folderItem.toTreeItem(state.settings[folderItem.uid]);
        const siblings = nodeMap.has(folder.parent) ? nodeMap.get(folder.parent) : [];
        const children = nodeMap.has(folder.uid) ? nodeMap.get(folder.uid) : [];
        siblings.push(folder);
        siblings.sort(Folder.compare);
        children.sort(Folder.compare);
        folder.children = children;
        nodeMap.set(folder.parent, siblings);
        nodeMap.set(folder.uid, children);
    });
    return nodeMap.get(null) || [];
}

export function flat(state) {
    return state.folders.map(folderItem => folderItem.toTreeItem(state.settings[folderItem.uid]));
}

export function currentFolder(state) {
    return state.settings.current;
}

export function currentFolderId(state) {
    const folder = state.folders.find(folder => folder.uid === state.settings.current);
    if (folder) {
        return folder.internalId;
    }
    return null;
}

export function trashFolderId(state) {
    const trashFolder = state.folders.find(folderItem => folderItem.value.fullName === "Trash");
    if (trashFolder) {
        return trashFolder.internalId;
    }
    return null;
}
