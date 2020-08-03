import { MailBoxBuilder } from "./helpers/MailBoxBuilder";

export function mailshares(state, getters, rootState, rootGetters) {
    return getters["mailboxes/containers"]
        .filter(MailBoxBuilder.isMailshare)
        .map(mailbox => MailBoxBuilder.build(mailbox, rootGetters));
}
