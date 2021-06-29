import { allowSubfolder, isDefault, translatePath } from "~model/folder";

function fromMailboxFolder(remotefolder, mailbox) {
    const parent = remotefolder.value.parentUid;
    return {
        key: remotefolder.uid,
        remoteRef: {
            uid: remotefolder.uid,
            internalId: remotefolder.internalId
        },
        mailboxRef: {
            uid: mailbox.remoteRef.uid,
            key: mailbox.key
        },
        parent,
        name: isDefault(!parent, remotefolder.value.name, mailbox)
            ? translatePath(remotefolder.value.name)
            : remotefolder.value.name,
        imapName: remotefolder.value.name,
        path: remotefolder.value.fullName,
        writable: mailbox.writable,
        allowSubfolder: allowSubfolder(mailbox.writable, !parent, remotefolder.value.name, mailbox),
        default: isDefault(!parent, remotefolder.value.name, mailbox),
        expanded: false,
        unread: 0
    };
}

function toMailboxFolder(localfolder, mailbox) {
    return {
        uid: localfolder.key || localfolder.remoteRef.uid,
        internalId: localfolder.remoteRef.internalId,
        value: {
            parentUid: localfolder.parent,
            name: localfolder.name,
            fullName: localfolder.path.replace(new RegExp("^" + mailbox.root + "/"), "")
        }
    };
}

function toRef(payload) {
    if (typeof payload === "string") {
        const uid = payload;
        return { key: uid, uid };
    } else {
        const folder = payload;
        return { key: folder.key, uid: folder.remoteRef.uid };
    }
}

export const FolderAdaptor = {
    fromMailboxFolder,
    toMailboxFolder,
    toRef
};
