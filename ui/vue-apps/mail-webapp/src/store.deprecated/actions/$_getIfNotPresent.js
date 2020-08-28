export function $_getIfNotPresent({ dispatch, getters, rootGetters }, messageKeys) {
    const missingMessageKeys = messageKeys.filter(messageKey => !rootGetters["mail/isLoaded"](messageKey));
    return dispatch("messages/multipleByKey", missingMessageKeys).then(() =>
        getters["messages/getMessagesByKey"](messageKeys)
    );
}
