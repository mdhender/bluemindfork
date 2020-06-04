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
            owner: mailbox.owner,
            name: mailbox.name,
            verbs: mailbox.writable ? [Verb.Write] : [Verb.Read],
            type: "mailboxacl"
        };
    }
};

function fromUserMailbox(item) {
    return {
        ...fromBaseMailbox(item),
        name: item.name,
        type: MailboxType.USER,
        uid: "user." + item.name,
        key: "user." + item.name,
        root: ""
    };
}

function fromSharedMailbox(item) {
    return {
        ...fromBaseMailbox(item),
        name: item.ownerDisplayname,
        type: MailboxType.MAILSHARE,
        uid: item.owner,
        key: item.owner,
        root: item.ownerDisplayname
    };
}

function fromBaseMailbox(item) {
    return {
        owner: item.owner,
        writable: item.verbs.includes(Verb.Write) || item.verbs.includes(Verb.All)
    };
}
