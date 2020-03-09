export function deleteSelectedMessageKey(state, key) {
    const index = state.selectedMessageKeys.findIndex(selectedKey => selectedKey === key);
    if (index !== -1) {
        state.selectedMessageKeys.splice(index, 1);
    }
}
