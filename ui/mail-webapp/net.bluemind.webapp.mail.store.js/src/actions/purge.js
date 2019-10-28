import { AlertTypes, Alert } from "@bluemind/alert.store";
import UUIDGenerator from "@bluemind/uuid";

export function purge({ dispatch, commit }, { messageId, folderUid }) {
    let subject, loadingAlertUid = UUIDGenerator.generate();

    return dispatch("$_getIfNotPresent", { folder: folderUid, id: messageId })
        .then(message => {
            subject = message.subject;

            commit("alert/addAlert", new Alert({
                type: AlertTypes.LOADING,
                code: "ALERT_CODE_MSG_PURGE_LOADING",
                key : "common.alert.purge.loading",
                uid: loadingAlertUid,
                props: { subject }
            }), { root: true });
            
            return dispatch("messages/remove", { messageId, folderUid });
        })
        .then(() => {
            commit("alert/removeAlert", loadingAlertUid, { root: true });
            commit("alert/addAlert", new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_PURGE_OK",
                key: "common.alert.purge.ok",
                props: { subject }
            }), { root: true });
        })
        .catch(reason => {
            commit("alert/removeAlert", loadingAlertUid, { root: true });
            commit("alert/addAlert", new Alert({
                code: "ALERT_CODE_MSG_PURGE_ERROR",
                key: "common.alert.purge.error",
                props: { subject, reason }
            }), { root: true });
        });
}
