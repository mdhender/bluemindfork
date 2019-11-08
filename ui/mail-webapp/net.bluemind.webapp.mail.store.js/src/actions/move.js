import UUIDGenerator from "@bluemind/uuid";

export function move({ state, dispatch, getters, commit }, { messageId, folder }) {
    let subject, destination;
    const alertUid = UUIDGenerator.generate();
    commit("alert/add", {
        code: "MSG_MOVED_LOADING",
        props: { subject: "mySubject" },
        uid: alertUid
    }, { root: true });

    return Promise.resolve()
        .then(() => {
            if (folder.uid) {
                return folder;
            }
            return dispatch("folders/create", folder, { root: true });
        })
        .then(({ uid }) => {
            destination = state.folders.items.find(folder => folder.uid == uid);
            return dispatch("$_getIfNotPresent", { folder: state.currentFolderUid, id: messageId });
        })
        .then(message => {
            subject = message.subject;
            const sourceId = getters["folders/getFolderByUid"](state.currentFolderUid).internalId;
            return dispatch("messages/move", { sourceId, destinationId: destination.internalId, messageId });
        })
        .then(() => {
            commit("alert/add", {
                code: "MSG_MOVE_OK",
                props: { subject, folder, folderNameLink: "/mail/" + folder.uid + "/" }
            }, { root: true });
        })
        .catch(error =>
            commit("alert/add", {
                code: "MSG_MOVE_ERROR",
                props: { subject, folderName: folder.name, reason: error.message }
            }, { root: true })
        )
        .finally(() => commit("alert/remove", alertUid, { root: true }));
}