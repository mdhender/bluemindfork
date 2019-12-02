import UUIDGenerator from "@bluemind/uuid";

export function move({ dispatch, commit, getters }, { messageKey, folder }) {
    let subject;
    const alertUid = UUIDGenerator.generate();

    return dispatch("$_getIfNotPresent", messageKey)
        .then(message => {
            subject = message.subject;
            commit(
                "alert/add",
                {
                    code: "MSG_MOVED_LOADING",
                    props: { subject },
                    uid: alertUid
                },
                { root: true }
            );
            return (
                folder.key ||
                dispatch("folders/create", {
                    name: folder.value.name,
                    parentUid: null,
                    mailboxUid: getters.my.mailboxUid
                })
            );
        })
        .then(key => dispatch("$_move", { messageKey, destinationKey: key }))
        .then(() => {
            commit(
                "alert/add",
                {
                    code: "MSG_MOVE_OK",
                    props: { subject, folder: folder.value, folderNameLink: "/mail/" + folder.key + "/" }
                },
                { root: true }
            );
        })
        .catch(error =>
            commit(
                "alert/add",
                {
                    code: "MSG_MOVE_ERROR",
                    props: { subject, folderName: folder.value.name, reason: error.message }
                },
                { root: true }
            )
        )
        .finally(() => commit("alert/remove", alertUid, { root: true }));
}
