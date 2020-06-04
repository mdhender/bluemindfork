import { MailBoxBuilder } from "./helpers/MailBoxBuilder";

export function mailshares(state, getters) {
    return getters["mailboxes/containers"]
        .filter(MailBoxBuilder.isMailshare)
        .map(mailbox => MailBoxBuilder.build(mailbox, getters));
}
