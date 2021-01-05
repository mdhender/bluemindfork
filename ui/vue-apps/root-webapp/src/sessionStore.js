import { inject } from "@bluemind/inject";

const state = {
    userSettings: {}
};

const actions = {
    async FETCH_ALL_SETTINGS({ commit }) {
        const userSession = inject("UserSession");
        let settings = await inject("UserSettingsPersistence").get(userSession.userId);

        // set default settings if needed
        settings = {
            insert_signature: "true",
            logout_purge: "false",
            mail_message_list_style: "normal",
            mail_thread: "false",
            trust_every_remote_content: "false",
            ...settings
        };

        commit("SET_USER_SETTINGS", settings);
    },

    async UPDATE_ALL_SETTINGS({ commit }, userSettings) {
        const userSession = inject("UserSession");
        await inject("UserSettingsPersistence").set(userSession.userId, userSettings);
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
