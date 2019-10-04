import { AlertTypes, Alert } from "@bluemind/alert.store";
import UUIDGenerator from "@bluemind/uuid";

export function move({ state, dispatch, getters, commit }, { messageId, folder }) {
    let subject, destination;
    const alertUid = UUIDGenerator.generate();
    commit("alert/addAlert", getMoveInProgress(alertUid), { root: true });

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
            commit("alert/addAlert", getMoveOkAlert(subject, folder), { root: true });
        })
        .catch(error =>
            commit("alert/addAlert", getMoveErrorAlert(subject, folder.name, error.message), { root: true })
        )
        .finally(() => commit("alert/removeAlert", alertUid, { root: true }));
}

function getMoveInProgress(uid) {
    return new Alert({
        uid,
        code: "ALERT_CODE_MSG_MOVED_IN_PROGRESS",
        key: "mail.alert.move.in_progress",
        type: AlertTypes.LOADING,
        props: { subject: "mySubject" }
    });
}

function getMoveOkAlert(subject, folder) {
    return new Alert({
        type: AlertTypes.SUCCESS,
        code: "ALERT_CODE_MSG_MOVE_OK",
        key: "mail.alert.move.ok",
        props: {
            subject,
            folder,
            folderNameLink: "/mail/" + folder.uid + "/"
        }
    });
}

function getMoveErrorAlert(subject, folderName, reason) {
    return new Alert({
        type: AlertTypes.ERROR,
        code: "ALERT_CODE_MSG_MOVE_ERROR",
        key: "mail.alert.move.error",
        props: {
            subject,
            folderName,
            reason
        }
    });
}
