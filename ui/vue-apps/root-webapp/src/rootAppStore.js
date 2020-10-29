const state = {
    appState: "loading",
    showSettings: false
};

const mutations = {
    SET_APP_STATE: (state, appState) => (state.appState = appState),
    TOGGLE_SETTINGS: state => (state.showSettings = !state.showSettings)
};

export default {
    namespaced: true,
    mutations,
    state
};
