import { LoadingStatus } from "./loading-status";

export const MailboxType = {
    GROUP: "groups",
    MAILSHARE: "mailshares",
    USER: "users",
    isShared(type) {
        return [this.GROUP, this.MAILSHARE].includes(type);
    }
};

export function create({ owner, dn, address, type }) {
    switch (type) {
        case MailboxType.USER:
            return createUserMailbox({ owner, dn, address });
        case MailboxType.GROUP:
            return createGroupMailbox({ owner, dn, address });
        case MailboxType.MAILSHARE:
            return createSharedMailbox({ owner, dn, address });
    }
}

function createUserMailbox({ owner, dn, address }) {
    return {
        ...createBaseMailbox({ owner, name: address, dn, address, type: MailboxType.USER }),
        remoteRef: { uid: "user." + owner },
        key: "user." + owner,
        root: ""
    };
}

function createSharedMailbox({ owner, dn, address }) {
    return {
        ...createBaseMailbox({ owner, name: dn, dn, address, type: MailboxType.MAILSHARE }),
        remoteRef: { uid: owner },
        key: owner,
        root: dn
    };
}

function createGroupMailbox({ owner, dn, address }) {
    return {
        ...createBaseMailbox({ owner, name: dn, dn, address, type: MailboxType.GROUP }),
        remoteRef: { uid: owner },
        key: owner,
        root: dn
    };
}

function createBaseMailbox({ owner, name, dn, address, type }) {
    return {
        dn,
        address,
        owner,
        name,
        loading: LoadingStatus.NOT_LOADED,
        writable: true,
        type
    };
}

export default {
    create,
    MailboxType
};
