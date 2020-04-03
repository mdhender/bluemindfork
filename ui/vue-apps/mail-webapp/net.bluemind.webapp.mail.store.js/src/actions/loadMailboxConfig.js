import injector from "@bluemind/inject";

export function loadMailboxConfig({ getters, commit }) {
    injector
        .getProvider("MailboxesPersistence")
        .get()
        .getMailboxConfig(getters.my.uid)
        .then(mailboxConfig => {
            commit("setMaxMessageSize", mailboxConfig.messageMaxSize);
        });
}
