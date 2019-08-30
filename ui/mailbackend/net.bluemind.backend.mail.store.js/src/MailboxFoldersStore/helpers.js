export function toTreeItem(folder, settings) {
    return {
        uid: folder.uid,
        name: folder.value.name,
        fullname: folder.value.fullName,
        parent: folder.value.parentUid || null,
        expanded: (settings && settings.expanded) || false,
        children: []
    };
}

const defaultFolders = [ "INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

export function sortFolders(f1, f2) {
    const f1Weight = defaultFolders.indexOf(f1.name);
    const f2Weight = defaultFolders.indexOf(f2.name);
    if (f1Weight >= 0 && f2Weight >= 0) {
        return f1Weight - f2Weight;
    } else if (f1Weight >= 0 && f2Weight < 0) {
        return -1;
    } else if (f1Weight < 0 && f2Weight >= 0 ) {
        return 1;
    } else {
        return f1.name.localeCompare(f2.name);
    }
}
