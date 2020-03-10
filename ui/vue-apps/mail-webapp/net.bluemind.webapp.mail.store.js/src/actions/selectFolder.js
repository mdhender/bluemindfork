import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";

export function selectFolder({ dispatch, commit, state }, { folderKey, filter }) {
    const folderUid = ItemUri.item(folderKey);
    let shouldClearMessages = false;
    if (state.currentFolderKey !== folderKey) {
        if (state.currentFolderKey) {
            ContainerObserver.forget("mailbox_records", ItemUri.item(state.currentFolderKey));
        }
        shouldClearMessages = true;
        commit("setCurrentFolder", folderKey);
        ContainerObserver.observe("mailbox_records", folderUid);
    }

    commit("search/setStatus", "idle");
    commit("search/setPattern", null);
    commit("currentMessage/clear");

    if (state.messageFilter !== filter) {
        commit("setMessageFilter", filter);
        shouldClearMessages = true;
    }

    if (shouldClearMessages) {
        commit("messages/clearItems");
        commit("messages/clearParts");
    }

    return dispatch("messages/list", { sorted: state.sorted, folderUid, filter })
        .then(() => {
            const sorted = state.messages.itemKeys;
            return dispatch("messages/multipleByKey", sorted.slice(0, 100));
        })
        .then(() => dispatch("loadUnreadCount", folderUid));
}
