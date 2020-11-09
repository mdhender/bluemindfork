import { REMOVE_MESSAGES } from "~actions";

export function remove({ dispatch }, messageKeys) {
    return dispatch("mail/" + REMOVE_MESSAGES, messageKeys, { root: true });
}
