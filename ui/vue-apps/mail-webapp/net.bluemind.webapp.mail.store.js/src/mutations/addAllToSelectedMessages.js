export function addAllToSelectedMessages(state) {
    state.selectedMessageKeys.splice(0);
    state.selectedMessageKeys.push(...state.messages.itemKeys);
}
