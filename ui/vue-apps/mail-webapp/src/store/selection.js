import {
    CLEAR_MESSAGE_LIST,
    REMOVE_MESSAGES,
    SET_MESSAGE_LIST,
    SELECT_MESSAGE,
    UNSELECT_MESSAGE,
    SELECT_ALL_MESSAGES,
    UNSELECT_ALL_MESSAGES
} from "./types/mutations";
import {
    IS_MESSAGE_SELECTED,
    MULTIPLE_MESSAGE_SELECTED,
    ONE_MESSAGE_SELECTED,
    IS_SELECTION_EMPTY
} from "./types/getters";

const state = [];

const mutations = {
    [SET_MESSAGE_LIST]: (state, messages) => {
        const keySet = new Set();
        messages.forEach(({ key }) => keySet.add(key));
        for (let index = state.length - 1; index >= 0; index--) {
            keySet.has(state[index]) || state.splice(index, 1);
        }
    },
    [REMOVE_MESSAGES]: (state, keys) => {
        for (let index = state.length - 1; index >= 0; index--) {
            keys.includes(state[index]) && state.splice(index, 1);
        }
    },
    [CLEAR_MESSAGE_LIST]: state => {
        state.splice(0);
    },
    [UNSELECT_MESSAGE]: (state, key) => {
        let index = state.indexOf(key);
        if (index >= 0) {
            state.splice(index, 1);
        }
    },
    [SELECT_MESSAGE]: (state, key) => {
        if (!state.includes(key)) state.push(key);
    },
    [SELECT_ALL_MESSAGES]: (state, keys) => {
        state.splice(0);
        keys.forEach(key => state.push(key));
    },
    [UNSELECT_ALL_MESSAGES]: state => {
        state.splice(0);
    }
};

const getters = {
    [IS_SELECTION_EMPTY]: state => state.length === 0,
    [ONE_MESSAGE_SELECTED]: state => state.length === 1,
    [MULTIPLE_MESSAGE_SELECTED]: state => state.length > 1,
    [IS_MESSAGE_SELECTED]: state => key => state.includes(key)
};

export default {
    state,
    getters,
    mutations
};
