const defaults = { INBOX: "INBOX", SENT: "Sent", DRAFTS: "Drafts", TRASH: "Trash", JUNK: "Junk", OUTBOX: "Outbox" };

export function getDefaultFolders(state, getters) {
    return mailboxUid =>
        Object.assign(
            {},
            ...Object.entries(defaults).map(([key, value]) => {
                return { [key]: getters.getFolderByPath(value, mailboxUid) };
            })
        );
}
