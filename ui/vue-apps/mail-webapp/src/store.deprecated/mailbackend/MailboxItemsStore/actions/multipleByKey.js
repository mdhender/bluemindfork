import { FETCH_MESSAGE_METADATA } from "~actions";

export function multipleByKey({ dispatch, rootState }, messageKeys) {
    return dispatch("mail/" + FETCH_MESSAGE_METADATA, { messageKeys, folders: rootState.mail.folders }, { root: true });
}
