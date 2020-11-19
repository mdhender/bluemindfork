import {
    CLEAR_MESSAGE_LIST,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SELECT_ALL_MESSAGES,
    SELECT_MESSAGE,
    SET_MESSAGE_LIST,
    UNSELECT_ALL_MESSAGES,
    UNSELECT_MESSAGE
} from "~mutations";
import { MESSAGE_IS_SELECTED, MULTIPLE_MESSAGE_SELECTED, ONE_MESSAGE_SELECTED, SELECTION_IS_EMPTY } from "~getters";

const state = [];

const mutations = {
    [SET_MESSAGE_LIST]: (state, messages) => {
        const keySet = new Set();
        messages.forEach(({ key }) => keySet.add(key));
        for (let index = state.length - 1; index >= 0; index--) {
            keySet.has(state[index]) || state.splice(index, 1);
        }
    },
    [REMOVE_MESSAGES]: (state, messages) => {
        const keySet = new Set();
        messages.forEach(({ key }) => keySet.add(key));
        for (let index = state.length - 1; index >= 0; index--) {
            keySet.has(state[index]) && state.splice(index, 1);
        }
    },
    [MOVE_MESSAGES]: (state, { messages }) => {
        const keySet = new Set();
        messages.forEach(({ key }) => keySet.add(key));
        for (let index = state.length - 1; index >= 0; index--) {
            keySet.has(state[index]) && state.splice(index, 1);
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
    [SELECTION_IS_EMPTY]: state => state.length === 0,
    [ONE_MESSAGE_SELECTED]: state => state.length === 1,
    [MULTIPLE_MESSAGE_SELECTED]: state => state.length > 1,
    [MESSAGE_IS_SELECTED]: state => key => state.includes(key)
};

export default {
    state,
    getters,
    mutations
};
