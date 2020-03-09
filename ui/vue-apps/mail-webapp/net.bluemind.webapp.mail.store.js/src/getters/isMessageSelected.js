export function isMessageSelected(state) {
    return key => state.selectedMessageKeys.find(selectedKey => selectedKey === key) !== undefined;
}
