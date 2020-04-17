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
    return dispatch("$_getIfNotPresent", [messageKey])
        .then(messages => {
            message = messages[0];
            subject = message.subject;
            commit(
                "addApplicationAlert",
                {
                    code: "MSG_REMOVED_LOADING",
                    props: { subject },
                    uid: loadingAlertUid
                },
                { root: true }
            );
            return dispatch("$_move", { messageKeys: [messageKey], destinationKey: getters.my.TRASH.key });
        })
        .then(() => {
            if (message.states.includes("not-seen")) {
                commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            }
            commit("addApplicationAlert", { code: "MSG_REMOVED_OK", props: { subject } }, { root: true });
        })
        .catch(reason =>
            commit("addApplicationAlert", { code: "MSG_REMOVED_ERROR", props: { subject, reason } }, { root: true })
        )
        .finally(() => commit("removeApplicationAlert", loadingAlertUid, { root: true }));
}
