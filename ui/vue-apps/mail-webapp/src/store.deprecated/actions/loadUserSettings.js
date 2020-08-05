import injector from "@bluemind/inject";

export function loadUserSettings({ commit }) {
    const userId = injector.getProvider("UserSession").get().userId;
    return injector
        .getProvider("UserSettingsPersistence")
        .get()
        .getOne(userId, "mail_message_list_style")
        .then(listStyleValue => {
            commit("setUserSettings", { mail_message_list_style: listStyleValue });
        });
}
