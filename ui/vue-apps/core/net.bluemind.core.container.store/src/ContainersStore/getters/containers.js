import { MailboxAdaptor } from "@bluemind/webapp.mail.store";

export function containers(state, getters, rootState) {
    return Object.values(rootState.mail.mailboxes).map(MailboxAdaptor.toMailboxContainer);
}
