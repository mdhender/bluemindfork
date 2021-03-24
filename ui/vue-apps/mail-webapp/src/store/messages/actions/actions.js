import apiFolders from "../../api/apiFolders";
import { MESSAGE_IS_LOADED } from "~getters";
import { MessageStatus, partialCopy } from "~model/message";
import apiMessages from "../../api/apiMessages";
import {
    ADD_FLAG,
    ADD_MESSAGES,
    DELETE_FLAG,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SET_MESSAGES_STATUS,
    SET_MESSAGES_LOADING_STATUS
} from "~mutations";
import { LoadingStatus } from "../../../model/loading-status";

export async function addFlag({ commit, getters }, { messages, flag }) {
    messages = Array.isArray(messages) ? messages : [messages];
    const toUpdate = messages.filter(({ key, flags }) => !getters[MESSAGE_IS_LOADED](key) || !flags.includes(flag));
    const locals = toUpdate.filter(({ key }) => getters[MESSAGE_IS_LOADED](key));
    commit(ADD_FLAG, { messages: locals, flag });
    try {
        await apiMessages.addFlag(toUpdate, flag);
    } catch (e) {
        commit(DELETE_FLAG, { messages: locals, flag });
        throw e;
    }
}

export async function deleteFlag({ commit, getters }, { messages, flag }) {
    messages = Array.isArray(messages) ? messages : [messages];
    const toUpdate = messages.filter(({ key, flags }) => !getters[MESSAGE_IS_LOADED](key) || flags.includes(flag));
    const locals = toUpdate.filter(({ key }) => getters[MESSAGE_IS_LOADED](key));
    commit(DELETE_FLAG, { messages: locals, flag });
    try {
        await apiMessages.deleteFlag(toUpdate, flag);
    } catch (e) {
        commit(ADD_FLAG, { messages: locals, flag });
        throw e;
    }
}

export async function fetchMessageMetadata({ state, commit }, messages) {
    messages = Array.isArray(messages) ? messages : [messages];
    const toFetch = messages.filter(({ composing }) => !composing);
    commit(
        SET_MESSAGES_LOADING_STATUS,
        messages
            .filter(({ key }) => state[key].loading !== LoadingStatus.LOADED)
            .map(message => ({ ...message, loading: LoadingStatus.LOADING }))
    );
    const fullMessages = await apiMessages.multipleById(toFetch);
    commit(ADD_MESSAGES, fullMessages);
    commit(
        SET_MESSAGES_LOADING_STATUS,
        messages
            .filter(({ key }) => state[key].loading !== LoadingStatus.LOADED)
            .map(message => ({ ...message, loading: LoadingStatus.ERROR }))
    );
}

export async function removeMessages({ commit, state }, messages) {
    messages = Array.isArray(messages) ? messages : [messages];
    const partial = messages.map(message => partialCopy(message));
    commit(
        SET_MESSAGES_STATUS,
        messages.map(message => ({ ...message, status: MessageStatus.REMOVED }))
    );

    try {
        await apiMessages.multipleDeleteById(messages);
        commit(REMOVE_MESSAGES, messages);
    } catch (e) {
        commit(
            SET_MESSAGES_STATUS,
            partial.map(({ key, status }) => ({ ...state[key], status: status }))
        );
        throw e;
    }
}

export async function moveMessages({ commit, state }, { messages, folder }) {
    messages = Array.isArray(messages) ? messages : [messages];
    messages = messages.filter(({ folderRef: { key } }) => key !== folder.key);
    const partial = messages.map(message => partialCopy(message));
    commit(
        SET_MESSAGES_STATUS,
        messages.map(message => ({ ...message, status: MessageStatus.REMOVED }))
    );

    try {
        await apiMessages.move(messages, folder);
        commit(MOVE_MESSAGES, { messages, folder });
    } catch (e) {
        commit(
            SET_MESSAGES_STATUS,
            partial.map(({ key, status }) => ({ ...state[key], status: status }))
        );
        throw e;
    }
}

export async function emptyFolder({ commit, state }, { folder, mailbox }) {
    const messages = Object.values(state).filter(m => m.folderRef.key === folder.key);
    const partial = messages.map(message => partialCopy(message));

    commit(
        SET_MESSAGES_STATUS,
        messages.map(message => ({ ...message, status: MessageStatus.REMOVED }))
    );

    try {
        await apiFolders.emptyFolder(mailbox, folder);
        commit(REMOVE_MESSAGES, messages);
    } catch (e) {
        commit(
            SET_MESSAGES_STATUS,
            partial.map(({ key, status }) => ({ ...state[key], status: status }))
        );
        throw e;
    }
}
