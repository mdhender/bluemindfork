import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

export function purge({ dispatch, commit, state }, messageKey) {
    let subject,
        message,
        loadingAlertUid = UUIDGenerator.generate();
    return dispatch("$_getIfNotPresent", messageKey)
        .then(m => {
            message = m;
            subject = message.subject;

            commit("alert/add", {
                code: "MSG_PURGE_LOADING",
                uid: loadingAlertUid,
                props: { subject }
            }, { root: true });

            return dispatch("messages/remove", messageKey);
        })
        .then(() => {
            if (message.states.includes("not-seen")) {
                const folderUid = ItemUri.container(messageKey);
                commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            }
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_PURGE_OK", props: { subject } }, { root: true });
        })
        .catch(reason => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit("alert/add", { code: "MSG_PURGE_ERROR", props: { subject, reason } }, { root: true });
        });
}
