import mutationsTypes from "../mutationTypes";
import actionTypes from "../actionTypes";
import MessageStatus from "./MessageStatus";
import apiMessages from "../api/apiMessages";
import MessageAdaptor from "./MessageAdaptor";

export default {
    [actionTypes.ADD_FLAG]: async ({ commit, getters, state }, { messageKeys, flag }) => {
        const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
        const messages = keys
            .map(key => state[key])
            .filter(message => !getters.isLoaded(message.key) || !message.flags.includes(flag));
        commit(mutationsTypes.ADD_FLAG, { keys, flag });
        try {
            await apiMessages.addFlag(messages, flag);
        } catch (e) {
            commit(mutationsTypes.DELETE_FLAG, { keys: messages.map(({ key }) => key), flag });
            throw e;
        }
    },
    [actionTypes.DELETE_FLAG]: async ({ commit, state, getters }, { messageKeys, flag }) => {
        const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
        const messages = keys
            .map(key => state[key])
            .filter(message => !getters.isLoaded(message.key) || message.flags.includes(flag));
        commit(mutationsTypes.DELETE_FLAG, { keys: messages.map(({ key }) => key), flag });
        try {
            await apiMessages.deleteFlag(messages, flag);
        } catch (e) {
            commit(mutationsTypes.ADD_FLAG, { keys: messages.map(({ key }) => key), flag });
            throw e;
        }
    },
    [actionTypes.FETCH_MESSAGE_METADATA]: async ({ commit, state }, { messageKeys }) => {
        const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key => state[key]);
        const fullMessages = await apiMessages.multipleById(messages);
        commit(mutationsTypes.ADD_MESSAGES, fullMessages);
    },

    [actionTypes.REMOVE_MESSAGES]: async ({ commit, state }, messageKeys) => {
        const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key =>
            MessageAdaptor.partialCopy(state[key])
        );
        commit(
            mutationsTypes.SET_MESSAGES_STATUS,
            messages.map(({ key }) => ({ key, status: MessageStatus.REMOVED }))
        );

        try {
            await apiMessages.multipleDeleteById(messages);
            commit(
                mutationsTypes.REMOVE_MESSAGES,
                messages.map(({ key }) => key)
            );
        } catch (e) {
            commit(mutationsTypes.SET_MESSAGES_STATUS, messages);
            throw e;
        }
    }
};
