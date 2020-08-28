export function getMessagesByKey(state, getters) {
    return keys => keys.map(key => getMessageByKey(state, getters)(key)).filter(Boolean);
}

export function getMessageByKey(state, getters) {
    return key => getters.messages[getters.indexOf(key)];
}
