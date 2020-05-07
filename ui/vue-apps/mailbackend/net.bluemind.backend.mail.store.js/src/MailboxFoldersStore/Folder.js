const defaultFolders = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

export function isDefaultFolder(folder) {
    return defaultFolders.includes(folder.fullName);
}

const FORBIDDEN_FOLDER_CHARACTERS = "/@%*";

/**
 * return invalid character if name is invalid
 */
export function isFolderNameValid(name) {
    for (let i = 0; i < name.length; i++) {
        if (FORBIDDEN_FOLDER_CHARACTERS.includes(name.charAt(i))) {
            return name.charAt(i);
        }
    }
    return true;
}

export default class Folder {
    constructor(key, item) {
        Object.assign(this, item);
        this.key = key;
    }

    match(pattern) {
        let normalized = pattern.toLowerCase().replace(/\/+/g, "/");

        const path = this.value.fullName.toLowerCase();
        if (normalized.startsWith("/")) {
            normalized = "^" + normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.slice(0, -1) + "(/[^/]*)?$";
        } else {
            normalized += "[^/]*$";
        }
        return path.search(normalized) >= 0;
    }

    compare(folder) {
        const f1Weight = defaultFolders.indexOf(this.value.fullName);
        const f2Weight = defaultFolders.indexOf(folder.value.fullName);
        if (f1Weight >= 0 && f2Weight >= 0) {
            return f1Weight - f2Weight;
        } else if (f1Weight >= 0 && f2Weight < 0) {
            return -1;
        } else if (f1Weight < 0 && f2Weight >= 0) {
            return 1;
        } else {
            return this.value.fullName.localeCompare(folder.value.fullName);
        }
    }
}
