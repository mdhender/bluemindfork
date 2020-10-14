import {
    RESET_ACTIVE_MESSAGE,
    SET_ACTIVE_MESSAGE,
    SET_ACTIVE_MESSAGE_PART_DATA,
    UNSELECT_ALL_CONVERSATIONS,
    UNSELECT_CONVERSATION
} from "~/mutations";

export default {
    mutations: {
        [SET_ACTIVE_MESSAGE]: (state, { key }) => {
            state.key = key;
            state.partsDataByAddress = {};
        },
        [SET_ACTIVE_MESSAGE_PART_DATA]: (state, { address, data }) => {
            state.partsDataByAddress[address] = data;
        },
        [RESET_ACTIVE_MESSAGE]: state => {
            state.key = null;
            state.partsDataByAddress = {};
        },
        [UNSELECT_CONVERSATION]: (state, messageKey) => {
            if (state.key === messageKey) {
                state.key = null;
                state.partsDataByAddress = {};
            }
        },
        [UNSELECT_ALL_CONVERSATIONS]: state => {
            state.key = null;
            state.partsDataByAddress = {};
        }
    },

    state: {
        key: null
    }
};
