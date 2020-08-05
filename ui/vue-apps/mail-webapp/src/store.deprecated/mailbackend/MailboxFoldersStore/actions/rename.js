export async function rename({ dispatch, rootState }, { folder, newFolderName }) {
    const mailbox = rootState.mail.mailboxes[folder.mailbox];
    return dispatch("mail/RENAME_FOLDER", { key: folder.key, name: newFolderName, mailbox }, { root: true });
}
