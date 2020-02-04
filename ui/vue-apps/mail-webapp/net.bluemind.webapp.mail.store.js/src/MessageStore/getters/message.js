export function message(state, getters, rootState, rootGetters) {
    if (state.key !== undefined) {
        return rootGetters["mail-webapp/messages/getMessageByKey"](state.key);
    }
}
