import mutationTypes from "../../mutationTypes";
import { MessageStatus, partialCopy } from "../../../model/message";
import apiMessages from "../../api/apiMessages";

export async function addFlag({ commit, getters, state }, { messageKeys, flag }) {
    const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messages = keys
        .map(key => state[key])
        .filter(message => !getters.isLoaded(message.key) || !message.flags.includes(flag));
    commit(mutationTypes.ADD_FLAG, { keys, flag });
    try {
        await apiMessages.addFlag(messages, flag);
    } catch (e) {
        commit(mutationTypes.DELETE_FLAG, { keys, flag });
        throw e;
    }
}

export async function deleteFlag({ commit, state, getters }, { messageKeys, flag }) {
    const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messages = keys
        .map(key => state[key])
        .filter(message => !getters.isLoaded(message.key) || message.flags.includes(flag));
    commit(mutationTypes.DELETE_FLAG, { keys: messages.map(({ key }) => key), flag });
    try {
        await apiMessages.deleteFlag(messages, flag);
    } catch (e) {
        commit(mutationTypes.ADD_FLAG, { keys: messages.map(({ key }) => key), flag });
        throw e;
    }
}

export async function fetchMessageMetadata({ commit, state }, { messageKeys }) {
    const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys])
        .map(key => state[key])
        .filter(({ composing }) => !composing);
    let fullMessages = await apiMessages.multipleById(messages);
    fullMessages = fullMessages.map(message => {
        if (state[message.key]) {
            message.partContentByAddress = state[message.key].partContentByAddress;
        }
        return message;
    });
    commit(mutationTypes.ADD_MESSAGES, fullMessages);
}

export async function removeMessages({ commit, state }, messageKeys) {
    const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key => partialCopy(state[key]));
    commit(
        mutationTypes.SET_MESSAGES_STATUS,
        messages.map(({ key }) => ({ key, status: MessageStatus.REMOVED }))
    );

    try {
        await apiMessages.multipleDeleteById(messages);
        commit(
            mutationTypes.REMOVE_MESSAGES,
            messages.map(({ key }) => key)
        );
    } catch (e) {
        commit(mutationTypes.SET_MESSAGES_STATUS, messages);
        throw e;
    }
}
