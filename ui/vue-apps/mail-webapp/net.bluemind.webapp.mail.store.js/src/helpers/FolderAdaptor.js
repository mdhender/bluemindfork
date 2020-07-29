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
            path: path(mailbox.root, remotefolder.value.fullName),
            expanded: false,
            writable: mailbox.writable,
            default: FolderAdaptor.isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox)
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
            path: path((parent && parent.path) || mailbox.root, name),
            writable: mailbox.writable,
            default: FolderAdaptor.isDefault(!parent, name, mailbox)
        };
    },

    rename(folder, name) {
        const path = folder.path.replace(new RegExp(folder.name + "$"), name);
        return {
            ...folder,
            name,
            path
        };
    },

    toggle(folder) {
        return {
            ...folder,
            expanded: !folder.expanded
        };
    }
};

function path() {
    return Array.from(arguments)
        .filter(Boolean)
        .join("/");
}
