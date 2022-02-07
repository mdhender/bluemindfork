import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_PART_ADDRESS } from "~/mutations";

const state = {
    partAddress: null,
    messageKey: null
};

const mutations = {
    [SET_PREVIEW_PART_ADDRESS](state, partAddress) {
        state.partAddress = partAddress;
    },
    [SET_PREVIEW_MESSAGE_KEY](state, messageKey) {
        state.messageKey = messageKey;
    }
};

export default { state, mutations };
