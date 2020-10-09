import mutationTypes from "../../store/mutationTypes";

export function _removeMessages({ commit }, messageKeys) {
    messageKeys.forEach(key => commit("deleteSelectedMessageKey", key));
    commit("mail/" + mutationTypes.REMOVE_MESSAGES, messageKeys, { root: true });
}
