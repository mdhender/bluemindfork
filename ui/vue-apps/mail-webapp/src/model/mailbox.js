import { LoadingStatus } from "./loading-status";

export const MailboxType = {
    MAILSHARE: "mailshares",
    USER: "users"
};

export function create({ owner, name, type }) {
    switch (type) {
        case MailboxType.USER:
            return createUserMailbox({ owner, name });
        case MailboxType.MAILSHARE:
            return createSharedMailbox({ owner, name });
    }
}

function createUserMailbox({ owner, name }) {
    return {
        ...createBaseMailbox({ owner, name }),
        type: MailboxType.USER,
        remoteRef: {
            uid: "user." + owner
        },
        key: "user." + owner,
        root: ""
    };
}

function createSharedMailbox({ owner, name }) {
    return {
        ...createBaseMailbox({ owner, name }),
        type: MailboxType.MAILSHARE,
        remoteRef: {
            uid: owner
        },
        key: owner,
        root: name
    };
}

function createBaseMailbox({ owner, name }) {
    return {
        owner,
        name,
        loading: LoadingStatus.NOT_LOADED,
        writable: true
    };
}
