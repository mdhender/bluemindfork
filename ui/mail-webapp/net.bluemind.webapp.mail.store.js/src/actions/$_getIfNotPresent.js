export function $_getIfNotPresent({ state, dispatch, getters }, { folder, id }) {
    const items = state.messages.items;
    if (!items[id]) {
        return dispatch("messages/getCompleteById", { folder, id }).then(() => getters["messages/getMessageById"](id));
    }
    return Promise.resolve(getters["messages/getMessageById"](id));
}
