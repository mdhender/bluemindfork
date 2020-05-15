import Vue from "vue";

export function toggleEditFolder(state, folderUid) {
    if (!state.foldersData[folderUid]) {
        Vue.set(state.foldersData, folderUid, { editing: true });
    } else if (!state.foldersData[folderUid].editing) {
        Vue.set(state.foldersData[folderUid], "editing", true);
    } else {
        Vue.set(state.foldersData[folderUid], "editing", !state.foldersData[folderUid].editing);
    }
}
