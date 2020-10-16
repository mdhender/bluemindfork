/** Parameter 'excludes' has priority over 'includes'. */
export function filterSelectedMessageKeys({ state, commit }, { includes, excludes }) {
    let filtered = includes ? state.selectedMessageKeys.filter(k => includes.includes(k)) : state.selectedMessageKeys;
    filtered = excludes ? state.selectedMessageKeys.filter(k => !excludes.includes(k)) : filtered;
    commit("addAllToSelectedMessages", filtered);
}
