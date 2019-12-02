const defaultFolders = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

export default class Folder {
    constructor(key, item) {
        Object.assign(this, item);
        this.key = key;
    }

    static compare(f1, f2) {
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

    toTreeItem(settings) {
        return {
            uid: this.uid,
            id: this.internalId,
            name: this.value.name,
            fullname: this.value.fullName,
            icon: this.icon,
            parent: this.value.parentUid || null,
            expanded: (settings && settings.expanded) || false,
            children: []
        };
    }
}
