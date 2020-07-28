import { FETCH_MAILBOXES } from "@bluemind/webapp.mail.store";

export function all({ dispatch }) {
    return dispatch(FETCH_MAILBOXES, null, { root: true });
}
