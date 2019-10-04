export function currentMessage(state, getters) {
    if (state.currentMessageId !== undefined) {
        return getters["messages/getMessageById"](state.currentMessageId);
    }
}
