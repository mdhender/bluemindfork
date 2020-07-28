import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import UUIDGenerator from "@bluemind/uuid";

export function markAsRead(context, messageKeys) {
    const updateAction = "messages/addFlag";
    const unreadMessageFilter = message => message.states.includes("not-seen");
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_READ_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_READ_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_READ_ERROR"
    };
    return markAs(context, updateAction, unreadMessageFilter, alertCodes, messageKeys);
}

export function markAsUnread(context, messageKeys) {
    const updateAction = "messages/deleteFlag";
    const readMessageFilter = message => !message.states.includes("not-seen");
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_UNREAD_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_UNREAD_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_UNREAD_ERROR"
    };
    return markAs(context, updateAction, readMessageFilter, alertCodes, messageKeys);
}

function markAs(context, updateAction, messageFilter, alertCodes, messageKeys) {
    const alertUid = UUIDGenerator.generate();
    let promise;

    if (messageKeys.length > 1) {
        context.commit("addApplicationAlert", { code: alertCodes.LOADING, uid: alertUid }, { root: true });
    }

    if (anyMessageMissingInState(context.state, messageKeys)) {
        promise = markAsWhenMessagesMissingInState(messageKeys, updateAction, context.dispatch);
    } else {
        promise = markAsWhenAllMessagesAreInState(context, messageKeys, updateAction, messageFilter);
    }

    if (messageKeys.length > 1) {
        promise = promise
            .then(() => context.commit("addApplicationAlert", { code: alertCodes.SUCCESS }, { root: true }))
            .catch(() => context.commit("addApplicationAlert", { code: alertCodes.ERROR }, { root: true }))
            .finally(() => context.commit("removeApplicationAlert", alertUid, { root: true }));
    }
    return promise;
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
