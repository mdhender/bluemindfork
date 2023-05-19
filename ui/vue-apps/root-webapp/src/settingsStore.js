import Vue from "vue";
import { inject } from "@bluemind/inject";
import getPreferenceSections from "./components/preferences/sections";

const state = {};

const actions = {
    async FETCH_ALL_SETTINGS({ commit }, vm) {
        const userSession = inject("UserSession");
        const settings = await inject("UserSettingsPersistence").get(userSession.userId);
        const defaultSettings = extractDefaultValues(getPreferenceSections(vm));
        commit("SET_SETTINGS", { ...defaultSettings, ...settings });
    },

    async SAVE_SETTING({ state, commit }, { setting, value }) {
        const old = state[setting];
        commit("SET_SETTING", { setting, value });
        try {
            const userId = inject("UserSession").userId;
            await inject("UserSettingsPersistence").setOne(userId, setting, value);
        } catch (e) {
            commit("SET_SETTING", { setting, value: old });
            throw e;
        }
    }
};

const getters = {
    IS_COMPUTED_THEME_DARK: state => {
        switch (state.theme) {
            case "light":
                return false;
            case "dark":
                return true;
            default:
                return window.matchMedia("(prefers-color-scheme: dark)").matches;
        }
    },
    EXTRA_FONT_FAMILIES: state => {
        return state.domain_composer_font_stack.split(";").map(fontValue => {
            const fontName = fontValue.split(",")[0];
            return {
                id: fontName.toLowerCase(),
                text: fontName,
                value: fontValue.replaceAll(",", ", ")
            };
        });
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
    getters,
    mutations,
    state
};

function extractDefaultValues(sections) {
    const defaults = {};
    sections.forEach(section => {
        section.categories.forEach(category => {
            category.groups.forEach(group => {
                group.fields.forEach(field => {
                    if (field.component.options?.setting && field.component.options.default) {
                        defaults[field.component.options.setting] = field.component.options.default;
                    }
                });
            });
        });
    });
    return defaults;
}
