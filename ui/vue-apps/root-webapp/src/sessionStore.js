import cloneDeep from "lodash.clonedeep";
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
    settings: { remote: {}, local: {}, localHasErrors: [] }
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

    async SAVE_SETTING({ state, commit }, { setting, value }) {
        const old = state.settings.remote[setting];
        commit("SET_SETTING", { setting, value });
        try {
            const userId = inject("UserSession").userId;
            await inject("UserSettingsPersistence").setOne(userId, setting, JSON.stringify(value));
        } catch {
            commit("SET_SETTING", { setting, value: old });
        }
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
    SET_SETTING: (state, { setting, value }) => {
        state.settings.remote[setting] = value;
        state.settings.local[setting] = value;
    },
    SET_SETTINGS: (state, settings) => {
        state.settings.remote = cloneDeep(settings);
        state.settings.local = cloneDeep(settings);
    },
    REMOVE_LOCAL_HAS_ERROR: (state, fieldOnError) => {
        const index = state.settings.localHasErrors.findIndex(field => field === fieldOnError);
        if (index !== -1) {
            state.settings.localHasErrors.splice(index, 1);
        }
    },
    ROLLBACK_LOCAL_SETTINGS: state => {
        state.settings.local = cloneDeep(state.settings.remote);
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
