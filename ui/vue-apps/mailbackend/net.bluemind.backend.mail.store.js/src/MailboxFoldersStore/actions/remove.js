import ItemUri from "@bluemind/item-uri";
import { REMOVE_FOLDER } from "@bluemind/webapp.mail.store";

export async function remove({ dispatch, rootState }, folderKey) {
    const folderUid = ItemUri.item(folderKey);
    const folder = rootState.mail.folders[folderUid];
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    return dispatch(REMOVE_FOLDER, { key: folder.key, mailbox }, { root: true });
}
