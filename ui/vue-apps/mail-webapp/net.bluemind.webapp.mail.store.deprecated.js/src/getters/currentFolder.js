export function currentFolder(state, getters) {
    return getters["folders/getFolderByKey"](state.currentFolderKey);
}
