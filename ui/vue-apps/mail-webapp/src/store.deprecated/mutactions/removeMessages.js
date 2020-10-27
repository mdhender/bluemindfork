import mutationTypes from "../../store/mutationTypes";

export function _removeMessages({ commit }, messageKeys) {
    commit("mail/" + mutationTypes.REMOVE_MESSAGES, messageKeys, { root: true });
}
