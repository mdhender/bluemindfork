import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";

export function markAsRead(context, messageKeys) {
    const updateAction = "messages/addFlag";
    const unreadMessageFilter = message => message.states.includes("not-seen");
    return markAs(context, updateAction, unreadMessageFilter, messageKeys);
}

export function markAsUnread(context, messageKeys) {
    const updateAction = "messages/deleteFlag";
    const readMessageFilter = message => !message.states.includes("not-seen");
    return markAs(context, updateAction, readMessageFilter, messageKeys);
}

function markAs(context, updateAction, messageFilter, messageKeys) {
    if (anyMessageMissingInState(context.state, messageKeys)) {
        return markAsWhenMessagesMissingInState(messageKeys, updateAction, context.dispatch);
    } else {
        return markAsWhenAllMessagesAreInState(context, messageKeys, updateAction, messageFilter);
    }
}

function markAsWhenMessagesMissingInState(messageKeys, updateAction, dispatch) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    return updateFlag(updateAction, dispatch, messageKeys).then(() => loadUnreadCount(messageKeysByFolder, dispatch));
}

function markAsWhenAllMessagesAreInState(context, messageKeys, updateAction, messageFilter) {
    const messages = context.getters["messages/getMessagesByKey"](messageKeys);
    const filteredMessageKeys = filterMessages(messages, messageFilter);
    const messageKeysByFolder = ItemUri.urisByContainer(filteredMessageKeys);
    setUnreadCount(messageKeysByFolder, context.commit, context.state, updateAction);
    return updateFlag(updateAction, context.dispatch, filteredMessageKeys);
}

function filterMessages(messages, messageFilter) {
    return messages.filter(messageFilter).map(message => message.key);
}

function loadUnreadCount(messageKeysByFolder, dispatch) {
    return Promise.all(Object.keys(messageKeysByFolder).map(folderUid => dispatch("loadUnreadCount", folderUid)));
}

function setUnreadCount(messageKeysByFolder, commit, state, updateAction) {
    Object.keys(messageKeysByFolder).forEach(folder => {
        const length = messageKeysByFolder[folder].length;
        const value = updateAction === "messages/deleteFlag" ? length : -length;
        commit("setUnreadCount", {
            folderUid: folder,
            count: state.foldersData[folder].unread + value
        });
    });
}

function anyMessageMissingInState(state, messageKeys) {
    return messageKeys.filter(messageKey => !state.messages.items[messageKey]).length > 0;
}

function updateFlag(action, dispatch, messageKeys) {
    return dispatch(action, { messageKeys, mailboxItemFlag: Flag.SEEN });
}
