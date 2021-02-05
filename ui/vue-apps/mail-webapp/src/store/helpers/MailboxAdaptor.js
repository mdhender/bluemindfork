import { Verb } from "@bluemind/core.container.api";

export const MailboxType = {
    MAILSHARE: "mailshares",
    USER: "users"
};
export const MailboxAdaptor = {
    fromMailboxContainer(item) {
        const type = item.ownerDirEntryPath.split("/")[1];
        switch (type) {
            case MailboxType.USER:
                return fromUserMailbox(item);
            case MailboxType.MAILSHARE:
                return fromSharedMailbox(item);
        }
    },

    toMailboxContainer(mailbox) {
        return {
            ownerDirEntryPath: "/" + mailbox.type,
            ownerDisplayname: mailbox.name,
            owner: mailbox.owner,
            verbs: mailbox.writable ? [Verb.Write] : [Verb.Read],
            type: "mailboxacl"
        };
    }
};

function fromUserMailbox(item) {
    return {
        ...fromBaseMailbox(item),
        type: MailboxType.USER,
        remoteRef: {
            uid: "user." + item.owner
        },
        key: "user." + item.owner,
        root: "",
        offlineSync: item.offlineSync
    };
}

function fromSharedMailbox(item) {
    return {
        ...fromBaseMailbox(item),
        type: MailboxType.MAILSHARE,
        remoteRef: {
            uid: item.owner
        },
        key: item.owner,
        root: item.ownerDisplayname,
        offlineSync: item.offlineSync
    };
}

function fromBaseMailbox(item) {
    return {
        owner: item.owner,
        name: item.ownerDisplayname,
        writable: item.verbs.includes(Verb.Write) || item.verbs.includes(Verb.All)
    };
}
