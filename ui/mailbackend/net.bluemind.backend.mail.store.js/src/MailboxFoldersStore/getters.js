import { toTreeItem, sortFolders } from "./helpers";

export function tree(state) {
    const nodeMap = new Map();
    state.folders.forEach(folderItem => {
        const folder = toTreeItem(folderItem, state.settings[folderItem.uid]);
        const siblings = nodeMap.has(folder.parent) ? nodeMap.get(folder.parent) : [];
        const children = nodeMap.has(folder.uid) ? nodeMap.get(folder.uid) : [];
        siblings.push(folder);
        siblings.sort(sortFolders);
        children.sort(sortFolders);
        folder.children = children;
        nodeMap.set(folder.parent, siblings);
        nodeMap.set(folder.uid, children);
    });
    return nodeMap.get(null) || [];
}

export function currentFolder(state) {
    return state.settings.current;
}

export function currentFolderId(state) {
    const folder = state.folders.find(folder => folder.uid === state.settings.current);
    return folder && folder.internalId;
}

export function trashFolderId(state) {
    const folder = state.folders.find(folder => folder.uid === state.settings.current);
    return folder && folder.internalId;
}
