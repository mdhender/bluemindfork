export function nextMessageId(state, getters) {
    if (state.currentMessageId && getters["messages/count"] > 1) {
        const index = getters["messages/indexOf"](state.currentMessageId) + 1;
        return state.messages.sortedIds[index == getters["messages/count"] ? index - 2 : index];
    }
    return null;
}
