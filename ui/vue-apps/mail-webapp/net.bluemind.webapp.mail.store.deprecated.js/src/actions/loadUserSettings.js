import injector from "@bluemind/inject";

export function loadUserSettings({ rootGetters, commit }) {
    return injector
        .getProvider("UserSettingsPersistence")
        .get()
        .getOne(rootGetters.MY_MAILBOX_KEY, "mail_message_list_style")
        .then(listStyleValue => {
            commit("setUserSettings", { mail_message_list_style: listStyleValue });
        });
}
