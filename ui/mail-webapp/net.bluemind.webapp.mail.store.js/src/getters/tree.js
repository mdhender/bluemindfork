export function tree(state) {
    const nodeMap = new Map();
    state.folders.items.forEach(folderItem => {
        const data = state.foldersData[folderItem.uid] || {};
        const folder = toTreeItem(folderItem, data);
        const siblings = nodeMap.has(folder.parent) ? nodeMap.get(folder.parent) : [];
        const children = nodeMap.has(folder.uid) ? nodeMap.get(folder.uid) : [];
        siblings.push(folder);
        siblings.sort(compare);
        children.sort(compare);
        folder.children = children;
        nodeMap.set(folder.parent, siblings);
        nodeMap.set(folder.uid, children);
    });
    return nodeMap.get(null) || [];
}

function toTreeItem(folder, { unread, expanded }) {
    return {
        uid: folder.uid,
        name: folder.value.name,
        fullname: folder.value.fullName,
        parent: folder.value.parentUid || null,
        unread: unread || 0,
        expanded: !!expanded,
        children: []
    };
}

const defaultFolders = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

function compare(f1, f2) {
    const f1Weight = defaultFolders.indexOf(f1.name);
    const f2Weight = defaultFolders.indexOf(f2.name);
    if (f1Weight >= 0 && f2Weight >= 0) {
        return f1Weight - f2Weight;
    } else if (f1Weight >= 0 && f2Weight < 0) {
        return -1;
    } else if (f1Weight < 0 && f2Weight >= 0) {
        return 1;
    } else {
        return f1.name.localeCompare(f2.name);
    }
}
