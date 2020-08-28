export function message(state, getters, rootState, rootGetters) {
    if (state.key !== undefined) {
        var x = rootGetters["mail-webapp/messages/getMessageByKey"](state.key);
        return x;
    }
}
