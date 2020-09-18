import actionTypes from "../../../../store/actionTypes";

export function list({ dispatch, rootState }, { folderUid, filter }) {
    return dispatch(
        "mail/" + actionTypes.FETCH_FOLDER_MESSAGE_KEYS,
        {
            folder: rootState.mail.folders[folderUid],
            filter,
            conversationsEnabled: rootState.session.userSettings.mail_thread === "true"
        },
        { root: true }
    );
}
