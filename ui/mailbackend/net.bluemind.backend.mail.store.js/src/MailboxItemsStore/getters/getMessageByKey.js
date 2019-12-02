export function getMessageByKey(state, getters) {
    return key => getters.messages[getters.indexOf(key)];
}
