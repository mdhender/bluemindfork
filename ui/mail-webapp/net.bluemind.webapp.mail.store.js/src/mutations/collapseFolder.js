import Vue from "vue";

export function collapseFolder(state, folderKey) {
    if (!state.foldersData[folderKey]) {
        Vue.set(state.foldersData, folderKey, { expanded: false });
    } else {
        Vue.set(state.foldersData[folderKey], "expanded", false);
    }
}
