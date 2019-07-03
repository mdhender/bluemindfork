export function toTreeItem(folder, settings, selected) {
    return {
        uid: folder.uid,
        name: folder.value.name,
        fullname: folder.value.fullName,
        parent: folder.value.parentUid || null,
        expanded: (settings && settings.expanded) || false,
        selected,
        children: []
    };
}

export function sort(f1, f2) {
    const fn = name => {
        switch (name) {
            case "INBOX":
                return "00";
            case "Sent":
                return "01";
            case "Drafts":
                return "02";
            case "Trash":
                return "03";
            case "Junk":
                return "04";
            default:
                return name;
        }
    };
    return f1.name == f2.name ? 0 : fn(f1.name) > fn(f2.name) ? 1 : -1;
}
