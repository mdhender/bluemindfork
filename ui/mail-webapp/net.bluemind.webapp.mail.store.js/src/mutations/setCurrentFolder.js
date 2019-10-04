import ContainerObserver from "@bluemind/containerobserver";

export function setCurrentFolder(state, uid) {
    if (state.currentFolderUid) {
        ContainerObserver.forget("mailbox_records", state.currentFolderUid);
    }
    state.currentFolderUid = uid;
    ContainerObserver.observe("mailbox_records", uid);
}
