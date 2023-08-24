import { RESET_PREVIEW, SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import { SET_PREVIEW } from "~/actions";

const state = {
    messageKey: null,
    fileKey: null
};

const mutations = {
    [RESET_PREVIEW](state) {
        state.fileKey = null;
        state.messageKey = null;
    },
    [SET_PREVIEW_FILE_KEY](state, fileKey) {
        state.fileKey = fileKey;
    },
    [SET_PREVIEW_MESSAGE_KEY](state, messageKey) {
        state.messageKey = messageKey;
    }
};
const actions = {
    [SET_PREVIEW]({ commit }, { messageKey, fileKey }) {
        commit(SET_PREVIEW_FILE_KEY, fileKey);
        commit(SET_PREVIEW_MESSAGE_KEY, messageKey);
    }
};

export default { state, mutations, actions };
