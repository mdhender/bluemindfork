export function nextMessageKey(state, getters) {
    if (state.currentMessage.key && getters["messages/count"] > 1) {
        const index = getters["messages/indexOf"](state.currentMessage.key) + 1;
        return state.messages.itemKeys[index === getters["messages/count"] ? index - 2 : index];
    }
    return null;
}
