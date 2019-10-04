import Vue from "vue";

export function collapseFolder(state, folderUid) {
    if (!state.foldersData[folderUid]) {
        Vue.set(state.foldersData, folderUid, { expanded: false });
    } else {
        Vue.set(state.foldersData[folderUid], "expanded", false);
    }
}
