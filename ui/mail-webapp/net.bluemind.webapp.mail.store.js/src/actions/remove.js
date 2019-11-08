import UUIDGenerator from "@bluemind/uuid";

export function remove({ state, dispatch, getters, commit }, messageId) {
    let subject, loadingAlertUid = UUIDGenerator.generate();
    const destinationId = getters["folders/defaultFolders"].TRASH.internalId;
    const sourceId = getters["folders/getFolderByUid"](state.currentFolderUid).internalId;
    
    if (sourceId == destinationId) {
        return dispatch("purge", { messageId, folderUid: state.currentFolderUid });
    }
    return dispatch("$_getIfNotPresent", { folder: state.currentFolderUid, id: messageId })
        .then(message => {
            subject = message.subject;
            commit("alert/add", {  
                code: "MSG_REMOVED_LOADING",
                props: { subject },
                uid: loadingAlertUid
            }, { root: true });
            return dispatch("messages/move", { sourceId, destinationId, messageId });
        })
        .then(() => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_REMOVED_OK", props: { subject } }, { root: true });
        })
        .catch(reason => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_REMOVED_ERROR", props: { subject, reason } }, { root: true });
        });
}
