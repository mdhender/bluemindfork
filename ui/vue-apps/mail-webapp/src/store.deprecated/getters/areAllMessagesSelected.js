export function areAllMessagesSelected(state, getters, rootState) {
    const messageKeys = rootState.mail.messageList.messageKeys;
    return messageKeys.length > 0 && state.selectedMessageKeys.length === messageKeys.length;
}
