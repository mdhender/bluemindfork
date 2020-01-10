export function $_getIfNotPresent({ state, dispatch, getters }, messageKey) {
    const items = state.messages.items;
    if (!items[messageKey]) {
        return dispatch("messages/getCompleteByKey", messageKey).then(() =>
            getters["messages/getMessageByKey"](messageKey)
        );
    }
    return Promise.resolve(getters["messages/getMessageByKey"](messageKey));
}
