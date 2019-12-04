import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";

export function selectFolder({ dispatch, commit, state }, folderKey) {
    const folderUid = ItemUri.item(folderKey);
    if (state.currentFolderKey != folderKey) {
        if (state.currentFolderKey) {
            ContainerObserver.forget("mailbox_records", ItemUri.item(state.currentFolderKey));
        }
        commit("messages/clearItems");
        commit("messages/clearParts");
        commit("setCurrentFolder", folderKey);
        ContainerObserver.observe("mailbox_records", folderUid);
    }
    //FIXME
    commit("setSearchLoading", null);
    commit("setSearchPattern", null);
    commit("clearCurrentMessage");

    return dispatch("messages/list", { sorted: state.sorted, folderUid })
        .then(() => {
            const sorted = state.messages.itemKeys;
            return dispatch("messages/multipleByKey", sorted.slice(0, 100));
        })
        .then(() => dispatch("loadUnreadCount", folderUid));
}
