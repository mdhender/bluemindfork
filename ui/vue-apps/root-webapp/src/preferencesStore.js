const state = {
    offset: 0,
    showPreferences: false,
    selectedSectionCode: "",
    sectionByCode: {}
};

const mutations = {
    SET_OFFSET: (state, offset) => (state.offset = offset),
    TOGGLE_PREFERENCES: state => (state.showPreferences = !state.showPreferences),
    SET_SECTIONS: (state, sections = []) => {
        const sectionByCode = {};
        sections.forEach(s => (sectionByCode[s.code] = s));
        state.sectionByCode = sectionByCode;
    },
    SET_SELECTED_SECTION: (state, selectedPrefSection) => (state.selectedSectionCode = selectedPrefSection)
};

const getters = {
    SECTIONS: state => Object.values(state.sectionByCode)
};

export default {
    namespaced: true,
    mutations,
    state,
    getters
};
