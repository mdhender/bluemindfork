import { REMOVE_MESSAGES } from "~mutations";

export function _removeMessages({ commit }, messageKeys) {
    commit("mail/" + REMOVE_MESSAGES, messageKeys, { root: true });
}
