import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

export function remove({ dispatch, getters, commit, state }, messageKey) {
    let subject,
        message,
        loadingAlertUid = UUIDGenerator.generate();

    const folderUid = ItemUri.container(messageKey);
    if (getters.my.TRASH.uid === folderUid) {
        return dispatch("purge", messageKey);
    }
    return dispatch("$_getIfNotPresent", messageKey)
        .then(m => {
            message = m;
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
        .then(() => {
            if (message.states.includes("not-seen")) {
                commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            }
            commit("alert/add", { code: "MSG_REMOVED_OK", props: { subject } }, { root: true });
        })
        .catch(reason => commit("alert/add", { code: "MSG_REMOVED_ERROR", props: { subject, reason } }, { root: true }))
        .finally(() => commit("alert/remove", loadingAlertUid, { root: true }));
}
