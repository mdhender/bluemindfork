import { LoadingStatus } from "./loading-status";

export const MailboxType = {
    MAILSHARE: "mailshares",
    USER: "users"
};

export function create({ owner, dn, address, type }) {
    switch (type) {
        case MailboxType.USER:
            return createUserMailbox({ owner, dn, address });
        case MailboxType.MAILSHARE:
            return createSharedMailbox({ owner, dn, address });
    }
}

function createUserMailbox({ owner, dn, address }) {
    return {
        ...createBaseMailbox({ owner, name: address, dn, address }),
        type: MailboxType.USER,
        remoteRef: {
            uid: "user." + owner
        },
        key: "user." + owner,
        root: ""
    };
}

function createSharedMailbox({ owner, dn, address }) {
    return {
        ...createBaseMailbox({ owner, name: dn, dn, address }),
        type: MailboxType.MAILSHARE,
        remoteRef: {
            uid: owner
        },
        key: owner,
        root: dn
    };
}

function createBaseMailbox({ owner, name, dn, address }) {
    return {
        dn,
        address,
        owner,
        name,
        loading: LoadingStatus.NOT_LOADED,
        writable: true
    };
}

export default {
    create,
    MailboxType
};
