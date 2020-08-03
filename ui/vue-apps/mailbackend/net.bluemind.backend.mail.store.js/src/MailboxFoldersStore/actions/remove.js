import { REMOVE_FOLDER } from "@bluemind/webapp.mail.store";

export async function remove({ dispatch, rootState }, folderKey) {
    const folder = rootState.mail.folders[folderKey];
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    return dispatch(REMOVE_FOLDER, { key: folder.key, mailbox }, { root: true });
}
