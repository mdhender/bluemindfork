const state = {
    appState: "loading"
};

const mutations = {
    SET_APP_STATE: (state, appState) => (state.appState = appState)
};

export default {
    namespaced: true,
    mutations,
    state
};
