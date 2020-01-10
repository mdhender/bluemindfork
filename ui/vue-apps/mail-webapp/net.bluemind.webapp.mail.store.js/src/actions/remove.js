import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

export function remove({ dispatch, getters, commit }, messageKey) {
    let subject,
        loadingAlertUid = UUIDGenerator.generate();

    if (getters.my.TRASH.uid == ItemUri.container(messageKey)) {
        return dispatch("purge", messageKey);
    }
    return dispatch("$_getIfNotPresent", messageKey)
        .then(message => {
            subject = message.subject;
            commit(
                "alert/add",
                {
                    code: "MSG_REMOVED_LOADING",
                    props: { subject },
                    uid: loadingAlertUid
                },
                { root: true }
            );
            return dispatch("$_move", { messageKey, destinationKey: getters.my.TRASH.key });
        })
        .then(() => commit("alert/add", { code: "MSG_REMOVED_OK", props: { subject } }, { root: true }))
        .catch(reason => commit("alert/add", { code: "MSG_REMOVED_ERROR", props: { subject, reason } }, { root: true }))
        .finally(() => commit("alert/remove", loadingAlertUid, { root: true }));
}
