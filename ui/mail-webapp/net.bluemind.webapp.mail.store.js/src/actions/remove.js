import { AlertTypes, Alert } from "@bluemind/alert.store";

export function remove({ state, dispatch, getters, commit }, messageId) {
    let subject;
    return dispatch("$_getIfNotPresent", { folder: state.currentFolderUid, id: messageId })
        .then(message => {
            subject = message.subject;
            const destinationId = getters["folders/defaultFolders"].TRASH.internalId;
            const sourceId = getters["folders/getFolderByUid"](state.currentFolderUid).internalId;
            return dispatch("messages/move", { sourceId, destinationId, messageId });
        })
        .then(() => {
            const key = "common.alert.remove.ok";
            const success = new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_REMOVED_OK",
                key,
                // message: this.$t(key, { subject }),
                props: { subject }
            });
            commit("alert/addSuccess", success, { root: true });
        })
        .catch(reason => {
            const key = "common.alert.remove.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_REMOVED_ERROR",
                key,
                // message: this.$t(key, { subject, reason }),
                props: { subject, reason }
            });
            commit("alert/addError", error, { root: true });
        });
}
