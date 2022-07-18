import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";

const state = {
    fileKey: null,
    messageKey: null
};

const mutations = {
    [SET_PREVIEW_FILE_KEY](state, fileKey) {
        state.fileKey = fileKey;
    },
    [SET_PREVIEW_MESSAGE_KEY](state, messageKey) {
        state.messageKey = messageKey;
    }
};

export default { state, mutations };
