import actionTypes from "../../../../store/actionTypes";

export function addFlag({ dispatch }, { messageKeys, mailboxItemFlag }) {
    return dispatch("mail/" + actionTypes.ADD_FLAG, { messageKeys, flag: mailboxItemFlag }, { root: true });
}
