import { ADD_FLAG } from "~actions";

export function addFlag({ dispatch }, { messageKeys, mailboxItemFlag }) {
    return dispatch("mail/" + ADD_FLAG, { messageKeys, flag: mailboxItemFlag }, { root: true });
}
