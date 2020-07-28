import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";

export function selectFolder({ commit, dispatch, state }, folderKey) {
    const folderUid = ItemUri.item(folderKey);
    if (state.currentFolderKey !== folderKey) {
        if (state.currentFolderKey) {
            ContainerObserver.forget("mailbox_records", ItemUri.item(state.currentFolderKey));
        }
        commit("setCurrentFolder", folderKey);
        ContainerObserver.observe("mailbox_records", folderUid);
    }
    return dispatch("loadUnreadCount", folderUid);
}
