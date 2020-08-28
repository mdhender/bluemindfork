import { inject } from "@bluemind/inject";
import mutationsTypes from "../mutationTypes";
import actionTypes from "../actionTypes";
import MessageAdaptor from "./MessageAdaptor";
import MessageStatus from "./MessageStatus";

export default {
    [actionTypes.ADD_FLAG]: async ({ commit, getters, state }, { messageKeys, flag }) => {
        const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
        const messagesByFolder = Object.values(
            keys.map(key => MessageAdaptor.partialCopy(state[key], ["flags"])).reduce(groupByFolder, {})
        );

        commit(mutationsTypes.ADD_FLAG, { messageKeys, flag });
        await Promise.all(messagesByFolder.map(messages => addFlagByFolder(messages, flag, getters, commit)));
    },
    [actionTypes.DELETE_FLAG]: async ({ commit, state, getters }, { messageKeys, flag }) => {
        const keys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
        const messagesByFolder = Object.values(
            keys.map(key => MessageAdaptor.partialCopy(state[key], ["flags"])).reduce(groupByFolder, {})
        );

        commit(mutationsTypes.DELETE_FLAG, { messageKeys, flag });
        await Promise.all(messagesByFolder.map(messages => deleteFlagByFolder(messages, flag, getters, commit)));
    },
    [actionTypes.FETCH_MESSAGE_METADATA]: async ({ commit, state }, { messageKeys }) => {
        const messagesByFolder = Object.values(messageKeys.map(key => state[key]).reduce(groupByFolder, {}));
        return Promise.all(messagesByFolder.map(messages => getMessages(messages, commit)));
    },
    [actionTypes.REMOVE_MESSAGES]: async ({ commit, state }, messageKeys) => {
        messageKeys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
        const messagesByFolder = messageKeys
            .map(key => MessageAdaptor.partialCopy(state[key]))
            .reduce(groupByFolder, {});

        commit(
            mutationsTypes.SET_MESSAGES_STATUS,
            messageKeys.map(key => ({ key, status: MessageStatus.REMOVED }))
        );
        await Promise.all(Object.values(messagesByFolder).map(messages => removeByFolder(commit, messages)));
    }
};

async function getMessages(messages, commit) {
    const folder = messages[0].folderRef;
    const items = await inject("MailboxItemsPersistence", folder.uid).multipleById(
        messages.map(message => message.remoteRef.internalId)
    );
    const adapted = items.map(item => MessageAdaptor.fromMailboxItem(item, folder));
    commit(mutationsTypes.ADD_MESSAGES, adapted);
}

function groupByFolder(messagesByFolder, message) {
    return {
        ...messagesByFolder,
        [message.folderRef.uid]: (messagesByFolder[message.folderRef.uid] || []).concat(message)
    };
}

async function removeByFolder(commit, messages) {
    try {
        await inject("MailboxItemsPersistence", messages[0].folderRef.uid).multipleDeleteById(
            messages.map(message => message.remoteRef.internalId)
        );
        commit(
            mutationsTypes.REMOVE_MESSAGES,
            messages.map(message => message.key)
        );
    } catch {
        commit(mutationsTypes.SET_MESSAGES_STATUS, messages);
    }
}

async function deleteFlagByFolder(messages, flag, getters, commit) {
    const service = inject("MailboxItemsPersistence", messages[0].folderRef.uid);
    try {
        const itemsId = messages
            .filter(message => !getters.isLoaded(message.key) || message.flags.includes(flag))
            .map(message => message.remoteRef.internalId);
        if (itemsId.length > 0) {
            await service.deleteFlag({ itemsId, mailboxItemFlag: flag });
        }
    } catch {
        const messageKeys = messages
            .filter(message => getters.isLoaded(message.key) && message.flags.includes(flag))
            .map(message => message.key);
        commit(mutationsTypes.ADD_FLAG, { messageKeys, flag });
    }
}

async function addFlagByFolder(messages, flag, getters, commit) {
    const service = inject("MailboxItemsPersistence", messages[0].folderRef.uid);
    try {
        const itemsId = messages
            .filter(message => !(getters.isLoaded(message.key) && message.flags.includes(flag)))
            .map(message => message.remoteRef.internalId);
        if (itemsId.length > 0) {
            await service.addFlag({ itemsId, mailboxItemFlag: flag });
        }
    } catch {
        const messageKeys = messages
            .filter(message => getters.isLoaded(message.key) && !message.flags.includes(flag))
            .map(message => message.key);
        commit(mutationsTypes.DELETE_FLAG, { messageKeys, flag });
    }
}
