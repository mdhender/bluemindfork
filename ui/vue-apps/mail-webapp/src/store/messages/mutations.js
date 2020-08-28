import Vue from "vue";
import mutationTypes from "../mutationTypes";
import MessageStatus from "./MessageStatus";

export default {
    [mutationTypes.ADD_MESSAGES]: (state, messages) => {
        messages.forEach(message => {
            Vue.set(state, message.key, message);
        });
    },
    [mutationTypes.ADD_FLAG]: (state, { messageKeys, flag }) => {
        messageKeys.forEach(key => {
            if (state[key].status === MessageStatus.LOADED && !state[key].flags.includes(flag)) {
                state[key].flags.push(flag);
            }
        });
    },
    [mutationTypes.DELETE_FLAG]: (state, { messageKeys, flag }) => {
        messageKeys.forEach(key => {
            if (state[key].status === MessageStatus.LOADED && state[key].flags.includes(flag)) {
                state[key].flags = state[key].flags.filter(f => f !== flag);
            }
        });
    },
    [mutationTypes.REMOVE_MESSAGES]: (state, messageKeys) => {
        messageKeys.forEach(key => {
            Vue.delete(state, key);
        });
    },
    [mutationTypes.SET_MESSAGES_STATUS]: (state, messages) => {
        messages.forEach(m => (state[m.key].status = m.status));
    },
    [mutationTypes.SET_MESSAGE_LIST]: (state, messages) => {
        messages.filter(message => !state[message.key]).forEach(message => Vue.set(state, message.key, message));
    }
};
