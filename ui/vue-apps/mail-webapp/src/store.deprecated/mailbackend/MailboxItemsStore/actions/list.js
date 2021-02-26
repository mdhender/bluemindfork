import { FETCH_MESSAGE_LIST_KEYS } from "~actions";

export function list({ dispatch, rootState }, { folderUid, filter }) {
    return dispatch(
        "mail/" + FETCH_MESSAGE_LIST_KEYS,
        {
            folder: rootState.mail.folders[folderUid],
            filter,
            conversationsEnabled: rootState.session.settings.remote.mail_thread === "true"
        },
        { root: true }
    );
}
