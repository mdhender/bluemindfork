import ItemUri from "@bluemind/item-uri";
import { RENAME_FOLDER } from "@bluemind/webapp.mail.store";

export async function rename({ rootState, dispatch }, { folderKey, newFolderName }) {
    const [folderUid, mailboxUid] = ItemUri.decode(folderKey);
    const mailbox = rootState.mail.mailboxes[mailboxUid];
    return dispatch(RENAME_FOLDER, { key: folderUid, name: newFolderName, mailbox }, { root: true });
}
