import { DELETE_FLAG } from "~actions";

export function deleteFlag({ dispatch }, { messageKeys, mailboxItemFlag }) {
    return dispatch("mail/" + DELETE_FLAG, { messageKeys, flag: mailboxItemFlag }, { root: true });
}
