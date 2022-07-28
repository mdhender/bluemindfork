import { folderUtils, mailboxUtils } from "@bluemind/mail";
const { allowConversations, allowSubfolder, isDefault, translatePath } = folderUtils;
const { MailboxType } = mailboxUtils;

function fromMailboxFolder(remotefolder, mailbox) {
    const parent = remotefolder.value.parentUid;
    const path = remotefolder.value.fullName;
    if (mailbox.type === "groups") {
        console.log(">>>>>>>>>>>>>>> fromMailboxFolder ", { mailbox, remotefolder });
    }
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
            : mailbox.type === MailboxType.GROUP
            ? mailbox.name
            : remotefolder.value.name,
        imapName: remotefolder.value.name,
        path,
        writable: mailbox.writable,
        allowConversations: allowConversations(path, mailbox),
        allowSubfolder: allowSubfolder(mailbox.writable, !parent, remotefolder.value.name, mailbox),
        default: isDefault(!parent, remotefolder.value.name, mailbox),
        expanded: false,
        unread: undefined
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
