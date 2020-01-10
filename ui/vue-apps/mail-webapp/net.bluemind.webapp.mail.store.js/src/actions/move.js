import UUIDGenerator from "@bluemind/uuid";

export function move({ dispatch, commit, getters }, { messageKey, folder }) {
    let subject, destination;
    const alertUid = UUIDGenerator.generate();
    return dispatch("$_getIfNotPresent", messageKey)
        .then(message => {
            subject = message.subject;
            addLoadingAlert(commit, subject, alertUid);
            return dispatch("$_createFolder", folder);
        })
        .then(key => {
            destination = getters["folders/getFolderByKey"](key);
            return dispatch("$_move", { messageKey, destinationKey: key });
        })
        .then(() => addOkAlert(commit, subject, destination))
        .catch(error => addErrorAlert(commit, subject, folder, error))
        .finally(() => commit("alert/remove", alertUid, { root: true }));
}

function addErrorAlert(commit, subject, folder, error) {
    commit(
        "alert/add",
        {
            code: "MSG_MOVE_ERROR",
            props: { subject, folderName: folder.value.name, reason: error.message }
        },
        { root: true }
    );
}

function addOkAlert(commit, subject, folder) {
    commit(
        "alert/add",
        {
            code: "MSG_MOVE_OK",
            props: { subject, folder: folder.value, folderNameLink: "/mail/" + folder.key + "/" }
        },
        { root: true }
    );
}

function addLoadingAlert(commit, subject, alertUid) {
    commit(
        "alert/add",
        {
            code: "MSG_MOVED_LOADING",
            props: { subject },
            uid: alertUid
        },
        { root: true }
    );
}
