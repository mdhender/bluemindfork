import { MailboxType } from "./MailboxAdaptor";

const DEFAULT_FOLDER_NAMES = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];

export const FolderAdaptor = {
    fromMailboxFolder(remotefolder, mailbox) {
        return {
            key: remotefolder.uid,
            uid: remotefolder.uid,
            id: remotefolder.internalId,
            mailbox: mailbox.uid,
            parent: remotefolder.value.parentUid,
            name: remotefolder.value.name,
            path: computePathFromRemote(remotefolder, mailbox),
            writable: mailbox.writable,
            default: isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox),
            expanded: false,
            unread: 0
        };
    },
    toMailboxFolder(localfolder, mailbox) {
        return {
            uid: localfolder.uid,
            internalId: localfolder.id,
            value: {
                parentUid: localfolder.parent,
                name: localfolder.name,
                fullName: localfolder.path.replace(new RegExp("^" + mailbox.root + "/"), "")
            }
        };
    },

    isMyMailboxDefaultFolder(folder) {
        return !folder.parent && DEFAULT_FOLDER_NAMES.includes(folder.name);
    },

    isMailshareRoot(folder, mailbox) {
        return mailbox.type === "mailshares" && !folder.parent;
    },

    create(key, name, parent, mailbox) {
        return {
            key: key,
            uid: null,
            id: null,
            mailbox: mailbox.key,
            parent: parent ? parent.key : null,
            name,
            path: computePath(mailbox, name, parent),
            writable: mailbox.writable,
            default: isDefault(!parent, name, mailbox),
            expanded: false,
            unread: 0
        };
    },

    rename(folder, name) {
        const path = folder.path.replace(new RegExp(folder.name + "$"), name);
        return { ...folder, name, path };
    }
};

function computePathFromRemote(remotefolder, mailbox) {
    if (mailbox.type === "mailshares") {
        if (!remotefolder.value.parentUid) {
            return mailbox.root;
        }
        return path(mailbox.root, remotefolder.value.fullName);
    }
    return remotefolder.value.fullName;
}

function computePath(mailbox, name, parent) {
    if (parent) {
        return path(parent.path, name);
    } else if (mailbox.type === "mailshares") {
        return mailbox.root;
    } else {
        return name;
    }
}

function path() {
    return Array.from(arguments)
        .filter(Boolean)
        .join("/");
}

function isDefault(isRootFolder, name, mailbox) {
    return isRootFolder && (mailbox.type !== MailboxType.USER || DEFAULT_FOLDER_NAMES.includes(name));
}
