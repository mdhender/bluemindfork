export function getMessagesByKey(state, getters) {
    return keys => getters.messages.filter(message => message && keys.includes(message.key));
}
