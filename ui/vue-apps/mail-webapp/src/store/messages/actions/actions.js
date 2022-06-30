import { draftUtils, loadingStatusUtils, messageUtils } from "@bluemind/mail";
import { MESSAGE_IS_LOADED } from "~/getters";
import apiMessages from "../../api/apiMessages";
import {
    ADD_FLAG,
    ADD_MESSAGES,
    DELETE_FLAG,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SET_MESSAGES_LOADING_STATUS
} from "~/mutations";
import { FolderAdaptor } from "../../folders/helpers/FolderAdaptor";
import { FETCH_MESSAGE_METADATA } from "~/actions";
import { Flag } from "@bluemind/email";

const { draftKey } = draftUtils;
const { LoadingStatus } = loadingStatusUtils;
const { createOnlyMetadata, messageKey, partialCopy } = messageUtils;

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

export async function fetchMessageIfNotLoaded({ state, commit, dispatch }, { internalId, folder }) {
    const key = messageKey(internalId, folder.key);
    const pendingDraftKey = draftKey(folder);
    if (!state[key] && state[pendingDraftKey]?.remoteRef.internalId === internalId) {
        return state[pendingDraftKey];
    }
    if (!state[key]) {
        commit(ADD_MESSAGES, {
            messages: [createOnlyMetadata({ internalId, folder: FolderAdaptor.toRef(folder) })]
        });
    }
    if (state[key].loading === LoadingStatus.NOT_LOADED) {
        await dispatch(FETCH_MESSAGE_METADATA, { messages: key });
    }
    return state[key];
}

export async function fetchMessageMetadata({ state, commit }, { messages: messageKeys }) {
    const messages = [];
    messageKeys = new Set(Array.isArray(messageKeys) ? messageKeys : [messageKeys]);
    messageKeys.forEach(key => {
        const message = state[key];
        if (!message.composing) {
            // BM-17736 fetch message with LOADING status
            if (message.loading !== LoadingStatus.ERROR) {
                messages.push(message);
            }
        }
    });
    commit(
        SET_MESSAGES_LOADING_STATUS,
        messages.reduce((loadings, message) => {
            if (message.loading !== LoadingStatus.LOADED) {
                loadings.push({ ...message, loading: LoadingStatus.LOADING });
            }
            return loadings;
        }, [])
    );
    const results = (await apiMessages.multipleById(messages)).reduce((results, message) => {
        if (!state[message.key].version || state[message.key].version < message.version) {
            results.push({ ...message, conversationRef: state[message.key].conversationRef });
        }
        messageKeys.delete(message.key);
        return results;
    }, []);
    commit(ADD_MESSAGES, { messages: results });
    commit(
        SET_MESSAGES_LOADING_STATUS,
        messages.reduce((errors, message) => {
            if (messageKeys.has(message.key)) {
                errors.push({ ...message, loading: LoadingStatus.ERROR });
            }
            return errors;
        }, [])
    );
}

export async function removeMessages({ commit }, { messages }) {
    messages = Array.isArray(messages) ? messages : [messages];
    commit(REMOVE_MESSAGES, { messages });
    try {
        await apiMessages.addFlag(messages, Flag.DELETED);
    } catch (e) {
        commit(ADD_MESSAGES, { messages });
        throw e;
    }
}

export async function moveMessages({ commit }, { conversation, messages, folder }) {
    messages = Array.isArray(messages) ? messages : [messages];
    const filtered = [],
        partial = [],
        folderRef = FolderAdaptor.toRef(folder);
    for (let message of messages) {
        if (message.folderRef.key !== folderRef.key) {
            partial.push(partialCopy(message));
            filtered.push({ ...message, folderRef });
        }
    }
    commit(MOVE_MESSAGES, { conversation, messages: filtered });
    try {
        await apiMessages.move(partial, folder);
    } catch (e) {
        commit(MOVE_MESSAGES, { conversation, messages: partial });
        throw e;
    }
}
