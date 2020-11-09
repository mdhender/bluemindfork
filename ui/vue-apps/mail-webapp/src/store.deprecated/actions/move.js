import UUIDGenerator from "@bluemind/uuid";
import { MailboxType } from "../../store/helpers/MailboxAdaptor";
import { MY_MAILBOX_KEY } from "~getters";

export function move(context, { messageKey, folder }) {
    const isArray = Array.isArray(messageKey);
    if (isArray && messageKey.length > 1) {
        return moveMultipleMessages(context, { messageKeys: messageKey, folder });
    } else {
        messageKey = isArray ? messageKey[0] : messageKey;
        return moveSingleMessage(context, { messageKey, folder });
    }
}

function moveSingleMessage({ dispatch, commit, rootState, rootGetters }, { messageKey, folder }) {
    let subject, destination, isDestinationMailshare;
    return dispatch("$_getIfNotPresent", [messageKey])
        .then(messages => {
            subject = messages[0].subject;
            return dispatch("$_createFolder", { folder, mailboxUid: rootGetters["mail/" + MY_MAILBOX_KEY] });
        })
        .then(key => {
            destination = rootState.mail.folders[key];
            isDestinationMailshare =
                rootState.mail.mailboxes[destination.mailboxRef.key].type === MailboxType.MAILSHARE;
            return dispatch("$_move", { messageKeys: [messageKey], destinationKey: key });
        })
        .then(() => addOkAlert(commit, subject, destination, isDestinationMailshare))
        .catch(error => addErrorAlert(commit, subject, folder, error));
}

function moveMultipleMessages({ dispatch, commit, rootState, rootGetters }, { messageKeys, folder }) {
    let destination, isDestinationMailshare;
    const alertUid = UUIDGenerator.generate();
    addLoadingAlertForMultipleMessages(commit, messageKeys.length, alertUid);
    return dispatch("$_createFolder", { folder, mailboxUid: rootGetters["mail/" + MY_MAILBOX_KEY] })
        .then(key => {
            destination = rootState.mail.folders[key];
            isDestinationMailshare =
                rootState.mail.mailboxes[destination.mailboxRef.key].type === MailboxType.MAILSHARE;

            return dispatch("$_move", { messageKeys, destinationKey: key });
        })
        .then(() => addOkAlertForMultipleMessages(commit, messageKeys.length, destination, isDestinationMailshare))
        .catch(() => addErrorAlertForMultipleMessages(commit, folder))
        .finally(() => commit("alert/removeApplicationAlert", alertUid, { root: true }));
}

function addErrorAlert(commit, subject, folder, error) {
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_ERROR",
            props: { subject, folderName: folder.name, reason: error.message }
        },
        { root: true }
    );
}

function addErrorAlertForMultipleMessages(commit, folder) {
    commit(
        "alert/addApplicationAlert",
        { code: "MSG_MOVE_ERROR_MULTIPLE", props: { folderName: folder.name } },
        { root: true }
    );
}

function addOkAlert(commit, subject, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.path } : { folder: folder.path };
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_OK",
            props: {
                subject,
                folder,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addOkAlertForMultipleMessages(commit, count, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.path } : { folder: folder.path };
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_OK_MULTIPLE",
            props: {
                count,
                folder,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addLoadingAlertForMultipleMessages(commit, count, alertUid) {
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVED_LOADING_MULTIPLE",
            props: { count },
            uid: alertUid
        },
        { root: true }
    );
}
