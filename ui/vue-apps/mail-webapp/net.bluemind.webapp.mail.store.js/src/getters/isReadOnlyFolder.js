import { ItemUri } from "@bluemind/item-uri";

export function isReadOnlyFolder(state, getters) {
    return folderUid => {
        const folderKey = Object.entries(state.folders.items).find(([, folder]) => folder.uid === folderUid)[0];
        const mailbox = ItemUri.container(folderKey);
        if (mailbox === getters.my.mailboxUid) {
            return false;
        } else {
            const mailshare = getters.mailshares.find(m => m.uid === mailbox);
            return !mailshare.writable;
        }
    };
}
