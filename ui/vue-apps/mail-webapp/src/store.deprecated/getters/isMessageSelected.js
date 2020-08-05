export function isMessageSelected(state) {
    return key => state.selectedMessageKeys.some(selectedKey => selectedKey === key);
}
