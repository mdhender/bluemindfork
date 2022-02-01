import Vue from "vue";
import { inject } from "@bluemind/inject";

const newWebmailDefaultSettings = {
    always_show_from: "false",
    always_show_quota: "false",
    auto_select_from: "never",
    insert_signature: "true",
    logout_purge: "false",
    mail_message_list_style: "normal",
    mail_thread: "false",
    trust_every_remote_content: "false"
};

const otherDefaultSettings = {
    default_event_alert_mode: "Display"
};

const defaultSettings = { ...newWebmailDefaultSettings, ...otherDefaultSettings };

const state = {};

const actions = {
    async FETCH_ALL_SETTINGS({ commit }) {
        const userSession = inject("UserSession");
        const settings = await inject("UserSettingsPersistence").get(userSession.userId);
        commit("SET_SETTINGS", { ...defaultSettings, ...settings });
    },

    async SAVE_SETTING({ state, commit }, { setting, value }) {
        const old = state[setting];
        commit("SET_SETTING", { setting, value });
        try {
            const userId = inject("UserSession").userId;
            await inject("UserSettingsPersistence").setOne(userId, setting, JSON.stringify(value));
        } catch (e) {
            commit("SET_SETTING", { setting, value: old });
            throw e;
        }
    }
};

const mutations = {
    SET_SETTING: (state, { setting, value }) => {
        Vue.set(state, setting, value);
    },
    SET_SETTINGS: (state, settings) => {
        for (const setting in settings) {
            Vue.set(state, setting, settings[setting]);
        }
    }
};

export default {
    namespaced: true,
    actions,
    mutations,
    state
};
