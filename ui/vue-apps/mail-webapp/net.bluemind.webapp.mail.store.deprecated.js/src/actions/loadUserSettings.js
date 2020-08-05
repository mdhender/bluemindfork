import injector from "@bluemind/inject";

export function loadUserSettings({ rootGetters, commit }) {
    return injector
        .getProvider("UserSettingsPersistence")
        .get()
        .getOne(rootGetters["mail/MY_MAILBOX"].owner, "mail_message_list_style")
        .then(listStyleValue => {
            commit("setUserSettings", { mail_message_list_style: listStyleValue });
        });
}
