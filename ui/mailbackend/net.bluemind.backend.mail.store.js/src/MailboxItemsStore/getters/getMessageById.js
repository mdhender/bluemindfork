export function getMessageById(state, getters) {
    return id => {
        const index = getters.indexOf(id);
        return getters.messages[index];
    };
}
