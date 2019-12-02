export function getFolderByKey(state, getters) {
    return key => getters.folders[state.itemKeys.indexOf(key)];
}
