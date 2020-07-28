import injector from "@bluemind/inject";

export function loadUserSettings({ getters, commit }) {
    return injector
        .getProvider("UserSettingsPersistence")
        .get()
        .getOne(getters.my.uid, "mail_message_list_style")
        .then(listStyleValue => {
            commit("setUserSettings", { mail_message_list_style: listStyleValue });
        });
}
