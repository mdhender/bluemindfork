import mutationsTypes from "../../mutationTypes";
import { MessageStatus, partialCopy } from "../../../model/message";
import apiMessages from "../../api/apiMessages";

export async function addFlag({ commit, getters, state }, { messageKeys, flag }) {
    const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messages = keys
        .map(key => state[key])
        .filter(message => !getters.isLoaded(message.key) || !message.flags.includes(flag));
    commit(mutationsTypes.ADD_FLAG, { keys, flag });
    try {
        await apiMessages.addFlag(messages, flag);
    } catch (e) {
        commit(mutationsTypes.DELETE_FLAG, { keys, flag });
        throw e;
    }
}

export async function deleteFlag({ commit, state, getters }, { messageKeys, flag }) {
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
}

export async function fetchMessageMetadata({ commit, state }, { messageKeys }) {
    const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key => state[key]);
    const fullMessages = await apiMessages.multipleById(messages);
    commit(mutationsTypes.ADD_MESSAGES, fullMessages);
}

export async function removeMessages({ commit, state }, messageKeys) {
    const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key => partialCopy(state[key]));
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
