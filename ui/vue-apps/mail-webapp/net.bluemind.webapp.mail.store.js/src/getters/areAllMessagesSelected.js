export function areAllMessagesSelected(state) {
    if (state.messages.itemKeys.length > 0) {
        return state.selectedMessageKeys.length === state.messages.itemKeys.length;
    }
    return false;
}
