export function currentMessage(state, getters) {
    if (state.currentMessageKey !== undefined) {
        return getters["messages/getMessageByKey"](state.currentMessageKey);
    }
}
