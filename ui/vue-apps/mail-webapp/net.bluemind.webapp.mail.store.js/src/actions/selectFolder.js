import ContainerObserver from "@bluemind/containerobserver";
import ItemUri from "@bluemind/item-uri";
import { STATUS } from "../constants";

export async function selectFolder({ dispatch, commit, state }, { folderKey, filter }) {
    // TODO: remove this if when Mehdi's work on router is merged
    if (state.currentFolderKey === folderKey && state.messageFilter === filter) {
        console.log("selectFolder not triggerred.");
        return;
    }

    commit("setStatus", STATUS.LOADING);
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

    if (shouldClearMessages || state.search.pattern !== null) {
        commit("deleteAllSelectedMessages");
    }

    if (shouldClearMessages) {
        commit("messages/clearItems");
        commit("messages/clearParts");
    }

    await dispatch("messages/list", { sorted: state.sorted, folderUid, filter });
    const sorted = state.messages.itemKeys;
    await dispatch("messages/multipleByKey", sorted.slice(0, 100));
    const result = await dispatch("loadUnreadCount", folderUid);

    commit("setStatus", STATUS.RESOLVED);
    return result;
}
