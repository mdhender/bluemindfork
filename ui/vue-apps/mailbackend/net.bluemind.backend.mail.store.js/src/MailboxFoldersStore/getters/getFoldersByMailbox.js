import ItemUri from "@bluemind/item-uri";

export function getFoldersByMailbox(state, getters) {
    const byMailbox = {};
    getters.folders.forEach(folder => {
        const mailbox = ItemUri.container(folder.key);
        const mailboxItems = byMailbox[mailbox] || [];
        mailboxItems.push(folder);
        byMailbox[mailbox] = mailboxItems;
    });
    return mailbox => byMailbox[mailbox] || [];
}
