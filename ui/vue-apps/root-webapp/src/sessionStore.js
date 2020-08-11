import injector from "@bluemind/inject";

const state = {
    userSettings: {}
};

const actions = {
    async FETCH_ALL_SETTINGS({ commit }) {
        const userSession = injector.getProvider("UserSession").get();
        let settings = await injector
            .getProvider("UserSettingsPersistence")
            .get()
            .get(userSession.userId);

        // set default settings if needed
        settings = { mail_message_list_style: "normal", mail_thread: "false", ...settings };

        commit("SET_USER_SETTINGS", settings);
    },

    async UPDATE_ALL_SETTINGS({ commit }, userSettings) {
        const userSession = injector.getProvider("UserSession").get();
        await injector
            .getProvider("UserSettingsPersistence")
            .get()
            .set(userSession.userId, userSettings);
        commit("SET_USER_SETTINGS", userSettings);
    }
};

const mutations = {
    SET_USER_SETTINGS: (state, userSettings) => (state.userSettings = userSettings)
};

export default {
    namespaced: true,
    actions,
    mutations,
    state
};
