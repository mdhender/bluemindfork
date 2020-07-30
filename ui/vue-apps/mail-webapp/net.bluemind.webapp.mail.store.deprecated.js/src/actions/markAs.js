import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";
import { SET_UNREAD_COUNT } from "@bluemind/webapp.mail.store";
import UUIDGenerator from "@bluemind/uuid";

export function markAsRead(context, messageKeys) {
    const updateAction = "messages/addFlag";
    const unreadMessageFilter = message => message.states.includes("not-seen");
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_READ_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_READ_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_READ_ERROR"
    };
    const onSuccess = onSuccessForMarkAsReadOrUnread(messageKeys, context, updateAction);
    return markAs(context, updateAction, Flag.SEEN, unreadMessageFilter, alertCodes, messageKeys, onSuccess);
}

export function markAsUnread(context, messageKeys) {
    const updateAction = "messages/deleteFlag";
    const readMessageFilter = message => !message.states.includes("not-seen");
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_UNREAD_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_UNREAD_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_UNREAD_ERROR"
    };
    const onSuccess = onSuccessForMarkAsReadOrUnread(messageKeys, context, updateAction);
    return markAs(context, updateAction, Flag.SEEN, readMessageFilter, alertCodes, messageKeys, onSuccess);
}

export function markAsFlagged(context, messageKeys) {
    const updateAction = "messages/addFlag";
    const unflaggedMessageFilter = message => !message.flags.includes(Flag.FLAGGED);
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_FLAGGED_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_FLAGGED_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_FLAGGED_ERROR"
    };
    return markAs(context, updateAction, Flag.FLAGGED, unflaggedMessageFilter, alertCodes, messageKeys);
}

export function markAsUnflagged(context, messageKeys) {
    const updateAction = "messages/deleteFlag";
    const flaggedMessageFilter = message => message.flags.includes(Flag.FLAGGED);
    const alertCodes = {
        LOADING: "MSG_MULTIPLE_MARK_AS_UNFLAGGED_LOADING",
        SUCCESS: "MSG_MULTIPLE_MARK_AS_UNFLAGGED_SUCCESS",
        ERROR: "MSG_MULTIPLE_MARK_AS_UNFLAGGED_ERROR"
    };
    return markAs(context, updateAction, Flag.FLAGGED, flaggedMessageFilter, alertCodes, messageKeys);
}

function onSuccessForMarkAsReadOrUnread(messageKeys, context, updateAction) {
    const messages = context.getters["messages/getMessagesByKey"](messageKeys);
    const filteredMessageKeys = filterMessages(messages, message => {
        const unread = message.states.includes("not-seen");
        return updateAction === "messages/deleteFlag" ? !unread : unread;
    });
    return () => {
        if (anyMessageMissingInState(context.state, messageKeys)) {
            const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
            loadUnreadCount(messageKeysByFolder, context.dispatch);
        } else {
            setUnreadCount(context, filteredMessageKeys, updateAction);
        }
    };
}

function markAs(context, updateAction, flagType, messageFilter, alertCodes, messageKeys, onSuccess) {
    const alertUid = UUIDGenerator.generate();
    let promise;

    if (messageKeys.length > 1) {
        context.commit("addApplicationAlert", { code: alertCodes.LOADING, uid: alertUid }, { root: true });
    }

    if (anyMessageMissingInState(context.state, messageKeys)) {
        promise = markAsWhenMessagesMissingInState(messageKeys, updateAction, flagType, context.dispatch);
    } else {
        promise = markAsWhenAllMessagesAreInState(context, messageKeys, updateAction, flagType, messageFilter);
    }

    promise = promise.then(onSuccess);

    if (messageKeys.length > 1) {
        promise = promise
            .then(() => context.commit("addApplicationAlert", { code: alertCodes.SUCCESS }, { root: true }))
            .catch(() => context.commit("addApplicationAlert", { code: alertCodes.ERROR }, { root: true }))
            .finally(() => context.commit("removeApplicationAlert", alertUid, { root: true }));
    }
    return promise;
}

function markAsWhenMessagesMissingInState(messageKeys, updateAction, flagType, dispatch) {
    return updateFlag(updateAction, flagType, dispatch, messageKeys);
}

function markAsWhenAllMessagesAreInState(context, messageKeys, updateAction, flagType, messageFilter) {
    const messages = context.getters["messages/getMessagesByKey"](messageKeys);
    const filteredMessageKeys = filterMessages(messages, messageFilter);
    return updateFlag(updateAction, flagType, context.dispatch, filteredMessageKeys);
}

function filterMessages(messages, messageFilter) {
    return messages.filter(messageFilter).map(message => message.key);
}

function loadUnreadCount(messageKeysByFolder, dispatch) {
    return Promise.all(Object.keys(messageKeysByFolder).map(folderUid => dispatch("loadUnreadCount", folderUid)));
}

function setUnreadCount(context, messageKeys, updateAction) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    Object.keys(messageKeysByFolder).forEach(folder => {
        const length = messageKeysByFolder[folder].length;
        const value = updateAction === "messages/deleteFlag" ? length : -length;
        context.commit(
            SET_UNREAD_COUNT,
            {
                key: folder,
                count: context.rootState.mail.folders[folder].unread + value //FIXME
            },
            { root: true }
        );
    });
}

function anyMessageMissingInState(state, messageKeys) {
    return messageKeys.filter(messageKey => !state.messages.items[messageKey]).length > 0;
}

function updateFlag(action, flagType, dispatch, messageKeys) {
    return dispatch(action, { messageKeys, mailboxItemFlag: flagType });
}
