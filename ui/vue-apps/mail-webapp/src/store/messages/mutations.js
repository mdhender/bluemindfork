import Vue from "vue";

export const ADD_MESSAGE = "ADD_MESSAGE";
const addMessage = (state, { key, ...message }) => {
    Vue.set(state, key, { ...message, key });
};

export const ADD_MESSAGES = "ADD_MESSAGES";
const addMessages = (state, messages) => {
    messages.forEach(message => {
        addMessage(state, message);
    });
};

export const UPDATE_STATUS = "UPDATE_STATUS";
const updateStatus = (state, { key, status }) => {
    const message = state[key];
    if (message) {
        message.status = status;
    }
};

export const MARK_AS_READ = "MARK_AS_READ";
const markAsRead = updateReadFlag(true);

export const MARK_AS_UNREAD = "MARK_AS_UNREAD";
const markAsUnread = updateReadFlag(false);

function updateReadFlag(isRead) {
    return (state, { key }) => {
        const message = state[key];
        if (message) {
            const flags = message.data && message.data.flags;
            const newLocal = {
                ...message.data,
                flags: { ...flags, read: isRead }
            };
            message.data = newLocal;
        }
    };
}

export default {
    [ADD_MESSAGE]: addMessage,
    [ADD_MESSAGES]: addMessages,
    [UPDATE_STATUS]: updateStatus,
    [MARK_AS_READ]: markAsRead,
    [MARK_AS_UNREAD]: markAsUnread
};

//TODO: Voici les mutations qui sont utilisées ci et là
// const addFlag = (state, { messageKeys, mailboxItemFlag }) => {};
// const deleteFlag = (state, { messageKeys, mailboxItemFlag }) => {};
// const storePartContent = (state, { messageKey, address, content }) => {};
// const storeItems = (state, { items, folderUid }) => {};
// const setItemKeysByIdsFolderUid = (state, { ids, folderUid }) => {};
// const setItemKeys = (state, itemKeys) => {};
// const removeItems = (state, messageKeys) => {};
