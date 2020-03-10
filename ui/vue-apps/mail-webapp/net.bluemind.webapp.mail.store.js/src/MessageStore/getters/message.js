export function message(state, getters, rootState, rootGetters) {
    if (state.key !== undefined) {
        return rootGetters["mail-webapp/messages/getMessagesByKey"]([state.key])[0];
    }
}
