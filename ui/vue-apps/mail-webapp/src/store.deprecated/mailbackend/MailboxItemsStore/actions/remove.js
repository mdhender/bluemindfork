import actionTypes from "../../../../store/actionTypes";

export function remove({ dispatch }, messageKeys) {
    return dispatch("mail/" + actionTypes.REMOVE_MESSAGES, messageKeys, { root: true });
}
