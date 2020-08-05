import injector from "@bluemind/inject";

export function loadMailboxConfig({ rootGetters, commit }) {
    injector
        .getProvider("MailboxesPersistence")
        .get()
        .getMailboxConfig(rootGetters.MY_MAILBOX_KEY)
        .then(mailboxConfig => {
            commit("setMaxMessageSize", mailboxConfig.messageMaxSize);
        });
}
