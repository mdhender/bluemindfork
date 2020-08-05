export async function remove({ dispatch, rootState }, folderKey) {
    const folder = rootState.mail.folders[folderKey];
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    return dispatch("mail/REMOVE_FOLDER", { key: folder.key, mailbox }, { root: true });
}
