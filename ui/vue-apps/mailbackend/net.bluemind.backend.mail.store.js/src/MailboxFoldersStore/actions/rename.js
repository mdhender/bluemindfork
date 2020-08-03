import { RENAME_FOLDER } from "@bluemind/webapp.mail.store";

export async function rename({ dispatch, rootState }, { folder, newFolderName }) {
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    return dispatch(RENAME_FOLDER, { key: folder.key, name: newFolderName, mailbox }, { root: true });
}
