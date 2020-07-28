export function $_getIfNotPresent({ state, dispatch, getters }, messageKeys) {
    const items = state.messages.items;
    const missingMessageKeys = messageKeys.filter(messageKey => !items[messageKey]);
    return dispatch("messages/multipleByKey", missingMessageKeys).then(() =>
        getters["messages/getMessagesByKey"](messageKeys)
    );
}
