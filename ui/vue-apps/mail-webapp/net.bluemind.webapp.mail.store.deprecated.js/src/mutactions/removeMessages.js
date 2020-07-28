export function _removeMessages({ commit }, messageKeys) {
    messageKeys.forEach(key => commit("deleteSelectedMessageKey", key));
    commit("messages/removeItems", messageKeys);
}
