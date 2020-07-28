import Folder from "../Folder";
import ItemUri from "@bluemind/item-uri";
import { FolderAdaptor } from "@bluemind/webapp.mail.store";

export function folders(state, getters, rootState) {
    return Object.values(rootState.mail.folders).map(folder => {
        const mailbox = rootState.mail.mailboxes[folder.mailbox];
        const key = ItemUri.encode(folder.uid, mailbox.uid);
        const parent = folder.parent && rootState.mail.folders[folder.parent];
        return new Folder(key, FolderAdaptor.toMailboxFolder(folder, parent, mailbox));
    });
}
