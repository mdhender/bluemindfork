import UUIDGenerator from "@bluemind/uuid";

export function purge({ dispatch, commit }, messageKey) {
    let subject,
        loadingAlertUid = UUIDGenerator.generate();
    return dispatch("$_getIfNotPresent", messageKey)
        .then(message => {
            subject = message.subject;

            commit("alert/add", {
                code: "MSG_PURGE_LOADING", 
                uid: loadingAlertUid, 
                props: { subject } 
            }, { root: true });
            
            return dispatch("messages/remove", messageKey);
        })
        .then(() => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_PURGE_OK", props: { subject } }, { root: true });
        })
        .catch(reason => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_PURGE_ERROR", props: { subject, reason }}, { root: true });
        });
}
