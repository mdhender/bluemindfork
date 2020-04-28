import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

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
    let subject, destination, isDestinationMailshare;
    const alertUid = UUIDGenerator.generate();
    return dispatch("$_getIfNotPresent", [messageKey])
        .then(messages => {
            subject = messages[0].subject;
            addLoadingAlert(commit, subject, alertUid);
            return dispatch("$_createFolder", folder);
        })
        .then(key => {
            destination = getters["folders/getFolderByKey"](key);
            const destinationContainer = ItemUri.container(destination.key);
            isDestinationMailshare = getters.mailshares.findIndex(({ uid }) => uid === destinationContainer) >= 0;
            return dispatch("$_move", { messageKeys: [messageKey], destinationKey: key });
        })
        .then(() => addOkAlert(commit, subject, destination, isDestinationMailshare))
        .catch(error => addErrorAlert(commit, subject, folder, error))
        .finally(() => commit("removeApplicationAlert", alertUid, { root: true }));
}

function moveMultipleMessages({ dispatch, commit, getters }, { messageKeys, folder }) {
    let destination, isDestinationMailshare;
    const alertUid = UUIDGenerator.generate();
    addLoadingAlertForMultipleMessages(commit, messageKeys.length, alertUid);
    return dispatch("$_createFolder", folder)
        .then(key => {
            destination = getters["folders/getFolderByKey"](key);
            const destinationContainer = ItemUri.container(destination.key);
            isDestinationMailshare = getters.mailshares.findIndex(({ uid }) => uid === destinationContainer) >= 0;
            return dispatch("$_move", { messageKeys, destinationKey: key });
        })
        .then(() => addOkAlertForMultipleMessages(commit, messageKeys.length, destination, isDestinationMailshare))
        .catch(() => addErrorAlertForMultipleMessages(commit, folder))
        .finally(() => commit("removeApplicationAlert", alertUid, { root: true }));
}

function addErrorAlert(commit, subject, folder, error) {
    commit(
        "addApplicationAlert",
        {
            code: "MSG_MOVE_ERROR",
            props: { subject, folderName: folder.value.name, reason: error.message }
        },
        { root: true }
    );
}

function addErrorAlertForMultipleMessages(commit, folder) {
    commit(
        "addApplicationAlert",
        { code: "MSG_MOVE_ERROR_MULTIPLE", props: { folderName: folder.value.name } },
        { root: true }
    );
}

function addOkAlert(commit, subject, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.value.fullName } : { folder: folder.value.fullName };
    commit(
        "addApplicationAlert",
        {
            code: "MSG_MOVE_OK",
            props: {
                subject,
                folder: folder.value,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addOkAlertForMultipleMessages(commit, count, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.value.fullName } : { folder: folder.value.fullName };
    commit(
        "addApplicationAlert",
        {
            code: "MSG_MOVE_OK_MULTIPLE",
            props: {
                count,
                folder: folder.value,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addLoadingAlert(commit, subject, alertUid) {
    commit(
        "addApplicationAlert",
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
        "addApplicationAlert",
        {
            code: "MSG_MOVED_LOADING_MULTIPLE",
            props: { count },
            uid: alertUid
        },
        { root: true }
    );
}
