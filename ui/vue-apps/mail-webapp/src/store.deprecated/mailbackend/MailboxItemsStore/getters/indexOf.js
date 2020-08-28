export function indexOf(state, getters, rootState) {
    return key => rootState.mail.messageList.messageKeys.findIndex(messageKey => messageKey === key);
}
