import { FETCH_MESSAGE_METADATA } from "~actions";

export function multipleByKey({ dispatch, rootState }, messageKeys) {
    const messages = messageKeys.map(key => rootState.mail.messages[key]);
    return dispatch("mail/" + FETCH_MESSAGE_METADATA, messages, { root: true });
}
