import actionTypes from "../../../../store/actionTypes";

export function deleteFlag({ dispatch }, { messageKeys, mailboxItemFlag }) {
    return dispatch("mail/" + actionTypes.DELETE_FLAG, { messageKeys, flag: mailboxItemFlag }, { root: true });
}
