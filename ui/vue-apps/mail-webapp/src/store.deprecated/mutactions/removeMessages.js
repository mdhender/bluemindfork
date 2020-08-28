import mutationTypes from "../../store/mutationTypes";

export function _removeMessages({ commit }, messageKeys) {
    messageKeys.forEach(key => commit("deleteSelectedMessageKey", key));
    commit("messages/removeParts", messageKeys);
    commit("mail/" + mutationTypes.REMOVE_MESSAGES, messageKeys, { root: true });
}
