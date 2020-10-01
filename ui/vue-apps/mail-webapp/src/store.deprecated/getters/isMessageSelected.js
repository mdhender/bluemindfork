export function isMessageSelected(state) {
    return key => state.selectedMessageKeys.includes(key);
}
