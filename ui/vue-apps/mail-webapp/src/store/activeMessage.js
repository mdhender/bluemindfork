import {
    RESET_ACTIVE_MESSAGE,
    SET_ACTIVE_MESSAGE,
    UNSELECT_ALL_CONVERSATIONS,
    UNSELECT_CONVERSATION
} from "~/mutations";

export default {
    mutations: {
        [SET_ACTIVE_MESSAGE]: (state, { key }) => {
            state.key = key;
        },
        [RESET_ACTIVE_MESSAGE]: state => {
            state.key = null;
        },
        [UNSELECT_CONVERSATION]: (state, messageKey) => {
            if (state.key === messageKey) {
                state.key = null;
            }
        },
        [UNSELECT_ALL_CONVERSATIONS]: state => {
            state.key = null;
        }
    },

    state: {
        key: null
    }
};
