import { MessageStatus, partialCopy } from "../../../model/message";
import apiMessages from "../../api/apiMessages";
import { MESSAGE_IS_LOADED } from "~getters";
import { ADD_FLAG, ADD_MESSAGES, DELETE_FLAG, REMOVE_MESSAGES, SET_MESSAGES_STATUS } from "~mutations";

export async function addFlag({ commit, getters, state }, { messageKeys, flag }) {
    const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messages = keys
        .map(key => state[key])
        .filter(message => !getters[MESSAGE_IS_LOADED](message.key) || !message.flags.includes(flag));
    commit(ADD_FLAG, { keys, flag });
    try {
        await apiMessages.addFlag(messages, flag);
    } catch (e) {
        commit(DELETE_FLAG, { keys, flag });
        throw e;
    }
}

export async function deleteFlag({ commit, state, getters }, { messageKeys, flag }) {
    const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    const messages = keys
        .map(key => state[key])
        .filter(message => !getters[MESSAGE_IS_LOADED](message.key) || message.flags.includes(flag));
    commit(DELETE_FLAG, { keys: messages.map(({ key }) => key), flag });
    try {
        await apiMessages.deleteFlag(messages, flag);
    } catch (e) {
        commit(ADD_FLAG, { keys: messages.map(({ key }) => key), flag });
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
    commit(ADD_MESSAGES, fullMessages);
}

export async function removeMessages({ commit, state }, messageKeys) {
    const messages = (Array.isArray(messageKeys) ? messageKeys : [messageKeys]).map(key => partialCopy(state[key]));
    commit(
        SET_MESSAGES_STATUS,
        messages.map(({ key }) => ({ key, status: MessageStatus.REMOVED }))
    );

    try {
        await apiMessages.multipleDeleteById(messages);
        commit(
            REMOVE_MESSAGES,
            messages.map(({ key }) => key)
        );
    } catch (e) {
        commit(SET_MESSAGES_STATUS, messages);
        throw e;
    }
}
