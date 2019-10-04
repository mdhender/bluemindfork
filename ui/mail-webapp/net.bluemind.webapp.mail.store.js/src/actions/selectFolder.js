import ContainerObserver from "@bluemind/containerobserver";

export function selectFolder({ dispatch, commit, state }, folderUid) {
    if (state.currentFolderUid != folderUid) {
        if (state.currentFolderUid) {
            ContainerObserver.forget("mailbox_records", state.currentFolderUid);
        }
        commit("messages/clearItems");
        commit("setCurrentFolder", folderUid);
    }
    //FIXME
    commit("setSearchLoading", null);
    commit("setSearchPattern", null);
    ContainerObserver.observe("mailbox_records", state.currentFolderUid);
    commit("clearCurrentMessage");
    // if (!getters["folders/getFolderByUid"](folderUid) && (const folder = getters["folders/getFolderByName"](name))) {
    //     folderUid = folder.uid;
    // }
    return dispatch("messages/sortedIds", { sorted: state.sorted, folderUid }).then(() => {
        const sorted = state.messages.sortedIds;
        return dispatch("messages/multipleById", { folderUid, ids: sorted.slice(0, 100) });
    });
}
