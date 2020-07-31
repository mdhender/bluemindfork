import { MailboxType } from "./MailboxAdaptor";

export const FolderAdaptor = {
    fromMailboxFolder(remotefolder, mailbox) {
        return {
            key: remotefolder.uid,
            uid: remotefolder.uid,
            id: remotefolder.internalId,
            mailbox: mailbox.uid,
            parent: remotefolder.value.parentUid,
            name: remotefolder.value.name,
            path:
                parent || mailbox.type !== "mailshares"
                    ? remotefolder.value.fullName
                    : path(mailbox.root, remotefolder.value.fullName),
            writable: mailbox.writable,
            default: FolderAdaptor.isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox),
            expanded: false,
            unread: 0
        };
    },
    toMailboxFolder(localfolder, parent, mailbox) {
        return {
            //TODO: Remove || localfolder.key when only the new store will bes used in MailFolderItemMenu. This is a temporary hack.
            uid: localfolder.uid || localfolder.key,
            internalId: localfolder.id,
            value: {
                parentUid: parent && parent.uid,
                name: localfolder.name,
                fullName: localfolder.path.replace(new RegExp("^" + mailbox.root + "/"), "")
            }
        };
    },

    isDefault(isRootFolder, name, mailbox) {
        const defaultFolderNames = ["INBOX", "Sent", "Drafts", "Trash", "Junk", "Outbox"];
        return isRootFolder && (mailbox.type !== MailboxType.USER || defaultFolderNames.includes(name));
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
            default: FolderAdaptor.isDefault(!parent, name, mailbox),
            expanded: false,
            unread: 0
        };
    },

    rename(folder, name) {
        const path = folder.path.replace(new RegExp(folder.name + "$"), name);
        return { ...folder, name, path };
    }
};

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
