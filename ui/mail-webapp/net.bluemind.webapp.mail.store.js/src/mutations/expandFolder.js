import Vue from "vue";

export function expandFolder(state, folderUid) {
    if (!state.foldersData[folderUid]) {
        Vue.set(state.foldersData, folderUid, { expanded: true });
    } else {
        Vue.set(state.foldersData[folderUid], "expanded", true);
    }
}
