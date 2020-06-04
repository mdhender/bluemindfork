import { FETCH_FOLDERS } from "@bluemind/webapp.mail.store";

export function all({ dispatch, rootState }, mailboxUid) {
    const mailbox = rootState.mail.mailboxes[mailboxUid];
    return dispatch(FETCH_FOLDERS, mailbox, { root: true });
}
