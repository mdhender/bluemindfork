const actions = {
    CANCEL({ state, commit }) {
        Object.keys(state).forEach(id => {
            if (state[id].current && !state[id].current.options.saved) {
                commit("PUSH_STATE", { id, ...state[id].saved });
            }
        });
    }
};

const getters = {
    ERRORS: state => Object.keys(state).filter(id => state[id]?.current?.options.error),
    HAS_CHANGED: state =>
        Object.values(state).some(
            ({ current }) => current?.options && !current.options.saved && !current.options.autosave
        ),
    HAS_ERROR: state => Object.values(state).some(({ current }) => current?.options.error),
    HAS_NOT_VALID: state => Object.values(state).some(({ current }) => current?.options.notValid),
    IS_RELOAD_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.reload),
    IS_LOGOUT_NEEDED: state => Object.values(state).some(({ saved }) => saved?.options.logout),
    NOT_VALID_PREFERENCES: state => Object.keys(state).filter(id => state[id]?.current?.options.notValid)
};

const mutations = {
    PUSH_STATE(state, { id, value, options = {} }) {
        if (options.saved) {
            state[id].saved = { value, options };
        }
        state[id].current = { value, options };
    },
    NEED_RELOAD(state, { id }) {
        if (state[id].saved) {
            state[id].saved.options.reload = true;
        } else {
            state[id].saved = { value: null, options: { reload: true } };
        }
    },
    NEED_LOGOUT(state, { id }) {
        if (state[id].saved) {
            state[id].saved.options.logout = true;
        } else {
            state[id].saved = { value: null, options: { logout: true } };
        }
    }
};

export default {
    actions,
    getters,
    mutations,
    namespaced: true,
    state: {}
};
