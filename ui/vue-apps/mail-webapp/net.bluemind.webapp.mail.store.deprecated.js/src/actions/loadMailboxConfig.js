import injector from "@bluemind/inject";

export function loadMailboxConfig({ rootGetters, commit }) {
    return injector
        .getProvider("MailboxesPersistence")
        .get()
        .getMailboxConfig(rootGetters["mail/MY_MAILBOX"].owner)
        .then(mailboxConfig => {
            commit("setMaxMessageSize", mailboxConfig.messageMaxSize);
        });
}
