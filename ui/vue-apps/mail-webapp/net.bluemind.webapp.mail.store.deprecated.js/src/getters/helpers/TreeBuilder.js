export const TreeBuilder = {
    build(nodes) {
        const nodeMap = new Map();
        nodes.forEach(node => {
            const siblings = nodeMap.has(node.parent) ? nodeMap.get(node.parent) : [];
            const children = nodeMap.has(node.uid) ? nodeMap.get(node.uid) : [];
            siblings.push(node);
            siblings.sort(compare);
            children.sort(compare);
            node.children = children;
            nodeMap.set(node.parent, siblings);
            nodeMap.set(node.uid, children);
        });
        return nodeMap.get(null) || [];
    },

    toTreeItem(folder, { unread, editing }, writable, expanded) {
        return {
            uid: folder.uid,
            key: folder.key,
            name: folder.value.name,
            fullName: folder.value.fullName,
            parent: folder.value.parentUid || null,
            unread: unread || 0,
            expanded: !!expanded,
            loaded: true,
            children: [],
            writable,
            editing: !!editing
        };
    }
};

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
