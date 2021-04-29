import {
    ADD_MESSAGES,
    CLEAR_MESSAGE_LIST,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SELECT_ALL_MESSAGES,
    SELECT_MESSAGE,
    SET_MESSAGE_LIST,
    UNSELECT_ALL_MESSAGES,
    UNSELECT_MESSAGE
} from "~mutations";
import {
    MESSAGE_IS_SELECTED,
    MULTIPLE_MESSAGE_SELECTED,
    ONE_MESSAGE_SELECTED,
    SELECTION_IS_EMPTY,
    SELECTION_KEYS
} from "~getters";

const state = {
    _keys: [],
    _removed: []
};

const mutations = {
    [UNSELECT_MESSAGE]: ({ _keys }, key) => {
        let index = _keys.indexOf(key);
        if (index >= 0) {
            _keys.splice(index, 1);
        }
    },
    [SELECT_MESSAGE]: ({ _keys }, key) => {
        if (!_keys.includes(key)) _keys.push(key);
    },
    [SELECT_ALL_MESSAGES]: (state, keys) => {
        state._keys = [...keys];
    },
    [UNSELECT_ALL_MESSAGES]: state => {
        state._keys = [];
        state._removed = [];
    },
    // Hooks
    [ADD_MESSAGES]: (state, messages) => {
        if (state._removed.length > 0) {
            const keys = new Set(messages.map(({ key }) => key));
            for (let i = state._removed.length - 1; i >= 0 && keys.size > 0; i--) {
                if (keys.has(state._removed[i])) {
                    keys.delete(state._removed[i]);
                    state._removed.splice(i, 1);
                }
            }
        }
    },
    [SET_MESSAGE_LIST]: (state, messages) => {
        const keySet = new Set(messages.map(({ key }) => key));
        const removed = new Set(state._removed);
        for (let index = state._keys.length - 1; index >= 0; index--) {
            if (!keySet.has(state._keys[index])) {
                removed.delete(state._keys[index]);
                state._keys.splice(index, 1);
            }
        }
        state._removed = Array.from(removed);
    },
    [REMOVE_MESSAGES]: ({ _keys, _removed }, messages) => {
        const keySet = new Set(messages.map(({ key }) => key));
        for (let i = 0; i < _keys.length && keySet.size > 0; i++) {
            if (keySet.has(_keys[i])) {
                keySet.delete(_keys[i]);
                _removed.push(_keys[i]);
            }
        }
    },
    [MOVE_MESSAGES]: (state, { messages }) => {
        const moved = new Set(messages.map(({ key }) => key));
        const removed = new Set(state._removed);
        for (let i = 0; i < state._keys.length && moved.size > 0; i++) {
            if (moved.has(state._keys[i])) {
                moved.delete(state._keys[i]);
                removed.has(state._keys[i]) ? removed.delete(state._keys[i]) : removed.add(state._keys[i]);
            }
        }
        state._removed = Array.from(removed);
    },
    [CLEAR_MESSAGE_LIST]: state => {
        state._keys = [];
        state._removed = [];
    }
};

const getters = {
    [SELECTION_IS_EMPTY]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length === 0,
    [ONE_MESSAGE_SELECTED]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length === 1,
    [MULTIPLE_MESSAGE_SELECTED]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length > 1,
    [MESSAGE_IS_SELECTED]: (s, { SELECTION_KEYS }) => {
        const selection = new Set(SELECTION_KEYS);
        return key => selection.has(key);
    },
    [SELECTION_KEYS]: ({ _keys, _removed }) => {
        if (_removed.length === 0) {
            return _keys;
        } else if (_removed.length <= _keys.length) {
            const removed = new Set(_removed);
            const result = [];
            for (let i = 0; i < _keys.length && removed.size > 0; i++) {
                removed.has(_keys[i]) ? removed.delete(_keys[i]) : result.push(_keys[i]);
            }
            return [...result, ..._keys.slice(result.length + _removed.length)];
        }
        return [];
    }
};

export default {
    state,
    getters,
    mutations
};
