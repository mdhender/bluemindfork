import {
    CLEAR_CONVERSATION_LIST,
    MOVE_MESSAGES,
    REMOVE_CONVERSATIONS,
    REMOVE_MESSAGES,
    SELECT_ALL_CONVERSATIONS,
    SELECT_CONVERSATION,
    SET_CONVERSATION_LIST,
    UNSELECT_ALL_CONVERSATIONS,
    UNSELECT_CONVERSATION
} from "~/mutations";
import {
    CONVERSATION_IS_SELECTED,
    SEVERAL_CONVERSATIONS_SELECTED,
    ONE_CONVERSATION_SELECTED,
    SELECTION_IS_EMPTY,
    SELECTION_KEYS
} from "~/getters";

const state = {
    _keys: [],
    _removed: []
};

const mutations = {
    [UNSELECT_CONVERSATION]: ({ _keys }, key) => {
        let index = _keys.indexOf(key);
        if (index >= 0) {
            _keys.splice(index, 1);
        }
    },
    [SELECT_CONVERSATION]: ({ _keys }, key) => {
        if (!_keys.includes(key)) _keys.push(key);
    },
    [SELECT_ALL_CONVERSATIONS]: (state, keys) => {
        state._keys = [...keys];
    },
    [UNSELECT_ALL_CONVERSATIONS]: state => {
        state._keys = [];
        state._removed = [];
    },
    // Hooks
    [SET_CONVERSATION_LIST]: (state, messages) => {
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
    [MOVE_MESSAGES]: (state, { conversation, messages }) => {
        if (conversation) {
            const messageKeysToRemove = new Set(messages.map(message => message.key));
            const countOfMessageInConversationFolder = conversation.messages
                .filter(message => message.folderRef.key === conversation.folderRef.key)
                .filter(message => !messageKeysToRemove.has(message.key)).length;
            if (countOfMessageInConversationFolder === 0) {
                state._removed.push(conversation.key);
            }
        }
    },
    [REMOVE_MESSAGES]: (state, { conversation, messages }) => {
        if (conversation) {
            const messageKeysToRemove = new Set(messages.map(message => message.key));
            const countOfMessageInConversationFolder = conversation.messages
                .filter(message => message.folderRef.key === conversation.folderRef.key)
                .filter(message => !messageKeysToRemove.has(message.key)).length;
            if (countOfMessageInConversationFolder === 0) {
                state._removed.push(conversation.key);
            }
        }
    },
    [REMOVE_CONVERSATIONS]: ({ _keys, _removed }, conversations) => {
        const keySet = new Set(conversations.map(({ key }) => key));
        for (let i = 0; i < _keys.length && keySet.size > 0; i++) {
            if (keySet.has(_keys[i])) {
                keySet.delete(_keys[i]);
                _removed.push(_keys[i]);
            }
        }
    },
    [CLEAR_CONVERSATION_LIST]: state => {
        state._keys = [];
        state._removed = [];
    }
};

const getters = {
    [SELECTION_IS_EMPTY]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length === 0,
    [ONE_CONVERSATION_SELECTED]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length === 1,
    [SEVERAL_CONVERSATIONS_SELECTED]: (s, { SELECTION_KEYS }) => SELECTION_KEYS.length > 1,
    [CONVERSATION_IS_SELECTED]: (s, { SELECTION_KEYS }) => {
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
