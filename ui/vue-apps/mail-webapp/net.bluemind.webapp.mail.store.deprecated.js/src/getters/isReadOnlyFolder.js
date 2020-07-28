import { ItemUri } from "@bluemind/item-uri";

export function isReadOnlyFolder(state, getters) {
    return folderUid => {
        const folder = getters["folders/folders"].find(folder => folder.uid === folderUid);
        const mailbox = ItemUri.container(folder.key);
        if (mailbox === getters.my.mailboxUid) {
            return false;
        } else {
            const mailshare = getters.mailshares.find(m => m.uid === mailbox);
            return !mailshare.writable;
        }
    };
}
