import { MailboxType } from "./mailbox";

export function create(key, name, parent, mailbox) {
    return {
        key: key,
        remoteRef: {
            uid: null,
            internalId: null
        },
        mailboxRef: {
            uid: mailbox.remoteRef.uid,
            key: mailbox.key
        },
        parent: parent ? parent.key : null,
        name: name,
        imapName: name,
        path: path(mailbox, name, parent),
        writable: mailbox.writable,
        default: isDefault(parent, name, mailbox),
        expanded: false,
        unread: 0
    };
}

export const DEFAULT_FOLDERS = {
    INBOX: "INBOX",
    SENT: "Sent",
    DRAFTS: "Drafts",
    TRASH: "Trash",
    JUNK: "Junk",
    OUTBOX: "Outbox"
};

function path(mailbox, name, parent) {
    if (parent) {
        return [parent.path, name].filter(Boolean).join("/");
    } else if (mailbox.type === MailboxType.MAILSHARE) {
        return mailbox.root;
    } else {
        return name;
    }
}

function isDefault(parent, name, mailbox) {
    return !parent && (mailbox.type !== MailboxType.USER || !!DEFAULT_FOLDERS[name]);
}
