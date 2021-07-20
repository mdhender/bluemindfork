import { inject } from "@bluemind/inject";

const newWebmailDefaultSettings = {
    always_show_from: "false",
    always_show_quota: "false",
    insert_signature: "true",
    logout_purge: "false",
    mail_message_list_style: "normal",
    mail_thread: "false",
    trust_every_remote_content: "false"
};

const otherDefaultSettings = {
    default_event_alert_mode: "Display"
};

const state = {
    settings: { remote: {}, local: {}, loaded: false, localHasErrors: [] }
};

const actions = {
    async FETCH_ALL_SETTINGS({ commit }) {
        const userSession = inject("UserSession");
        let settings = await inject("UserSettingsPersistence").get(userSession.userId);

        settings = {
            ...newWebmailDefaultSettings,
            ...otherDefaultSettings,
            ...settings
        };

        commit("SET_SETTINGS", settings);
    },

    async SAVE_SETTINGS({ state, commit }) {
        commit("SET_SETTINGS", state.settings.local);
        const userId = inject("UserSession").userId;
        return inject("UserSettingsPersistence").set(userId, state.settings.local);
    }
};

const mutations = {
    ADD_LOCAL_HAS_ERROR: (state, fieldOnError) => {
        const index = state.settings.localHasErrors.findIndex(field => field === fieldOnError);
        if (index === -1) {
            state.settings.localHasErrors.push(fieldOnError);
        }
    },
    SET_SETTINGS: (state, settings) => {
        state.settings.remote = JSON.parse(JSON.stringify(settings));
        state.settings.local = JSON.parse(JSON.stringify(settings));
        state.settings.loaded = true;
    },
    REMOVE_LOCAL_HAS_ERROR: (state, fieldOnError) => {
        const index = state.settings.localHasErrors.findIndex(field => field === fieldOnError);
        if (index !== -1) {
            state.settings.localHasErrors.splice(index, 1);
        }
    },
    ROLLBACK_LOCAL_SETTINGS: state => {
        state.settings.local = JSON.parse(JSON.stringify(state.settings.remote));
    }
};

const getters = {
    SETTINGS_CHANGED: state => JSON.stringify(state.settings.local) !== JSON.stringify(state.settings.remote)
};

export default {
    namespaced: true,
    actions,
    getters,
    mutations,
    state
};
