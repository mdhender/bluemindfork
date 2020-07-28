import Vue from "vue";

export function setUnreadCount(state, { folderUid, count }) {
    if (count >= 0) {
        if (!state.foldersData[folderUid]) {
            Vue.set(state.foldersData, folderUid, { unread: count });
        } else {
            Vue.set(state.foldersData[folderUid], "unread", count);
        }
    }
}
