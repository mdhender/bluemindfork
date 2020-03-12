export function addSelectedMessageKey(state, key) {
    if (!state.selectedMessageKeys.includes(key)) {
        state.selectedMessageKeys.push(key);
    }
}
