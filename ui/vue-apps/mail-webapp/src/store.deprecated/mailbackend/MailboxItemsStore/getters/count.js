export function count(state, getters, rootState) {
    return rootState.mail.messageList.messageKeys.length;
}
