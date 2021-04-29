import apiFolders from "../../api/apiFolders";
import { MESSAGE_IS_LOADED } from "~getters";
import { partialCopy } from "~model/message";
import apiMessages from "../../api/apiMessages";
import {
    ADD_FLAG,
    ADD_MESSAGES,
    DELETE_FLAG,
    MOVE_MESSAGES,
    REMOVE_MESSAGES,
    SET_MESSAGES_LOADING_STATUS
} from "~mutations";
import { LoadingStatus } from "../../../model/loading-status";
import { createOnlyMetadata, messageKey } from "../../../model/message";
import { FolderAdaptor } from "../../folders/helpers/FolderAdaptor";
import { FETCH_MESSAGE_METADATA } from "../../types/actions";
import { draftKey } from "../../../model/draft";

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
        commit(ADD_MESSAGES, [createOnlyMetadata({ internalId, folder: FolderAdaptor.toRef(folder) })]);
    }
    if (state[key].loading === LoadingStatus.NOT_LOADED) {
        await dispatch(FETCH_MESSAGE_METADATA, state[key]);
    }
    return state[key];
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
    const fullMessages = (await apiMessages.multipleById(toFetch)).filter(
        message => !state[message.key]?.version || state[message.key].version < message.version
    );
    commit(ADD_MESSAGES, fullMessages);
    commit(
        SET_MESSAGES_LOADING_STATUS,
        messages
            .filter(({ key }) => state[key].loading !== LoadingStatus.LOADED)
            .map(message => ({ ...message, loading: LoadingStatus.ERROR }))
    );
}

export async function removeMessages({ commit }, messages) {
    messages = Array.isArray(messages) ? messages : [messages];
    commit(REMOVE_MESSAGES, messages);
    try {
        await apiMessages.multipleDeleteById(messages);
    } catch (e) {
        commit(ADD_MESSAGES, messages);
        throw e;
    }
}

export async function moveMessages({ commit }, { messages, folder }) {
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
    commit(MOVE_MESSAGES, { messages: filtered });
    try {
        await apiMessages.move(partial, folder);
    } catch (e) {
        commit(MOVE_MESSAGES, { messages: partial });
        throw e;
    }
}

export async function emptyFolder({ commit, state }, { folder, mailbox }) {
    const messages = Object.values(state).filter(m => m.folderRef.key === folder.key);
    commit(REMOVE_MESSAGES, messages);
    try {
        await apiFolders.emptyFolder(mailbox, folder);
    } catch (e) {
        commit(ADD_MESSAGES, messages);
        throw e;
    }
}
