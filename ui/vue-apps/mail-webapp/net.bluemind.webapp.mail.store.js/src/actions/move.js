import UUIDGenerator from "@bluemind/uuid";

export function move({ dispatch, commit, getters }, { messageKey, folder }) {
    const isArray = Array.isArray(messageKey);
    if (isArray && messageKey.length > 1) {
        return moveMultipleMessages({ dispatch, commit, getters }, { messageKeys: messageKey, folder });
    } else {
        messageKey = isArray ? messageKey[0] : messageKey;
        return moveSingleMessage({ dispatch, commit, getters }, { messageKey, folder });
    }
}

function moveSingleMessage({ dispatch, commit, getters }, { messageKey, folder }) {
    let subject, destination;
    const alertUid = UUIDGenerator.generate();
    return dispatch("$_getIfNotPresent", [messageKey])
        .then(messages => {
            subject = messages[0].subject;
            addLoadingAlert(commit, subject, alertUid);
            return dispatch("$_createFolder", folder);
        })
        .then(key => {
            destination = getters["folders/getFolderByKey"](key);
            return dispatch("$_move", { messageKeys: [messageKey], destinationKey: key });
        })
        .then(() => addOkAlert(commit, subject, destination))
        .catch(error => addErrorAlert(commit, subject, folder, error))
        .finally(() => commit("alert/remove", alertUid, { root: true }));
}

function moveMultipleMessages({ dispatch, commit, getters }, { messageKeys, folder }) {
    let destination;
    const alertUid = UUIDGenerator.generate();
    addLoadingAlertForMultipleMessages(commit, messageKeys.length, alertUid);
    return dispatch("$_createFolder", folder)
        .then(key => {
            destination = getters["folders/getFolderByKey"](key);
            return dispatch("$_move", { messageKeys, destinationKey: key });
        })
        .then(() => addOkAlertForMultipleMessages(commit, messageKeys.length, destination))
        .catch(() => addErrorAlertForMultipleMessages(commit, folder))
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

function addErrorAlertForMultipleMessages(commit, folder) {
    commit("alert/add", { code: "MSG_MOVE_ERROR_MULTIPLE", props: { folderName: folder.value.name } }, { root: true });
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

function addOkAlertForMultipleMessages(commit, count, folder) {
    commit(
        "alert/add",
        {
            code: "MSG_MOVE_OK_MULTIPLE",
            props: { count, folder: folder.value, folderNameLink: "/mail/" + folder.key + "/" }
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

function addLoadingAlertForMultipleMessages(commit, count, alertUid) {
    commit(
        "alert/add",
        {
            code: "MSG_MOVED_LOADING_MULTIPLE",
            props: { count },
            uid: alertUid
        },
        { root: true }
    );
}
