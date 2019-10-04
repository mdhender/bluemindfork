const defaults = { INBOX: "INBOX", SENT: "Sent", DRAFTS: "Drafts", TRASH: "Trash", JUNK: "Junk", OUTBOX: "Outbox" };

export function defaultFolders(state, getters) {
    return Object.assign(
        {},
        ...Object.entries(defaults).map(([key, value]) => {
            return { [key]: getters.getFolderByName(value) };
        })
    );
}
