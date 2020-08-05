import injector from "@bluemind/inject";

export function loadMailboxConfig({ commit }) {
    const userId = injector.getProvider("UserSession").get().userId;
    return injector
        .getProvider("MailboxesPersistence")
        .get()
        .getMailboxConfig(userId)
        .then(mailboxConfig => {
            commit("setMaxMessageSize", mailboxConfig.messageMaxSize);
        });
}
