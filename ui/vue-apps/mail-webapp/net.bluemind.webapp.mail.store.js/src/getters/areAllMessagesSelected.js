export function areAllMessagesSelected(state) {
    return state.messages.itemKeys.length > 0 && state.selectedMessageKeys.length === state.messages.itemKeys.length;
}
