export function addSelectedMessageKey(state, key) {
    if (!state.selectedMessageKeys.includes(key)) {
        if (state.selectedMessageKeys.length === 0 && state.currentMessage.key && state.currentMessage.key !== key) {
            state.selectedMessageKeys.push(state.currentMessage.key);
        }
        state.selectedMessageKeys.push(key);
    }
}
