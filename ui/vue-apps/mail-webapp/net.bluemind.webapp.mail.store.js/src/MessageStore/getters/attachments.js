export function attachments(state, getters, rootState, rootGetters) {
    if (state.key) {
        return state.parts.attachments.map(part =>
            Object.assign({}, part, {
                content: rootGetters["mail-webapp/messages/getPartContent"](state.key, part.address)
            })
        );
    }
    return [];
}
