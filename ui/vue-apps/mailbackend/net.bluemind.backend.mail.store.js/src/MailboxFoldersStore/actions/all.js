export function all({ dispatch, rootState }, mailboxUid) {
    const mailbox = rootState.mail.mailboxes[mailboxUid];
    return dispatch("mail/FETCH_FOLDERS", mailbox, { root: true });
}
