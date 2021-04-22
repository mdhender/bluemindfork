import { inject } from "@bluemind/inject";

const state = {
    offset: 0,
    showPreferences: false,
    selectedSectionCode: "",
    sectionByCode: {},

    userPasswordLastChange: null
};

const actions = {
    async FETCH_USER_PASSWORD_LAST_CHANGE({ commit }) {
        const userId = inject("UserSession").userId;
        const user = await inject("UserClientPersistence").getComplete(userId);
        commit("SET_USER_PASSWORD_LAST_CHANGE", user);
    }
};

const mutations = {
    SET_OFFSET: (state, offset) => (state.offset = offset),
    TOGGLE_PREFERENCES: state => (state.showPreferences = !state.showPreferences),
    SET_SECTIONS: (state, sections = []) => {
        const sectionByCode = {};
        sections.forEach(s => (sectionByCode[s.code] = s));
        state.sectionByCode = sectionByCode;
    },
    SET_SELECTED_SECTION: (state, selectedPrefSection) => {
        state.selectedSectionCode = selectedPrefSection;
    },
    SET_USER_PASSWORD_LAST_CHANGE: (state, user) => {
        state.userPasswordLastChange = user.value.passwordLastChange || user.created;
    }
};

const getters = {
    SECTIONS: state => Object.values(state.sectionByCode)
};

export default {
    namespaced: true,
    actions,
    mutations,
    state,
    getters
};
