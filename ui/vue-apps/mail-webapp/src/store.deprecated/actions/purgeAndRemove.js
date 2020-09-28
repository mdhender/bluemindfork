import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

export function purge(context, messageKeys) {
    return action(context, messageKeys, "PURGE", purgeMessages);
}

export function remove(context, messageKeys) {
    let actionFunction = removeMessages;
    let type = "REMOVED";
    if (allMessagesInTrash(context.rootGetters, messageKeys)) {
        actionFunction = purgeMessages;
        type = "PURGE";
    }
    return action(context, messageKeys, type, actionFunction);
}

async function action(context, messageKeys, action, actionFunction) {
    messageKeys = [...(Array.isArray(messageKeys) ? messageKeys : [messageKeys])];
    const subject = await retrieveSubject(context.dispatch, messageKeys);
    let loadingAlertUid;
    if (messageKeys.length > 1) {
        loadingAlertUid = loadingAlert(action, context.commit, messageKeys, subject);
    }
    try {
        await actionFunction(context, messageKeys);
        cleanUp(messageKeys, context);
        okAlert(action, context.commit, messageKeys, subject);
    } catch (e) {
        errorAlert(action, context.commit, messageKeys, subject);
    } finally {
        if (messageKeys.length > 1) {
            context.commit("removeApplicationAlert", loadingAlertUid, { root: true });
        }
    }
}

function purgeMessages(context, messageKeys) {
    return context.dispatch("messages/remove", messageKeys);
}

function removeMessages(context, messageKeys) {
    return context.dispatch("$_move", {
        messageKeys: messageKeys,
        destinationKey: context.rootGetters["mail/MY_TRASH"].key
    });
}

function cleanUp(messageKeys, context) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    Object.keys(messageKeysByFolder).forEach(folderKey => {
        context.dispatch("loadUnreadCount", folderKey);
        const keys = messageKeysByFolder[folderKey];
        keys.forEach(messageKey => {
            context.commit("deleteSelectedMessageKey", messageKey);
            if (context.state.currentMessage.key === messageKey) {
                context.state.currentMessage.key = null;
            }
        });
    });
}

async function retrieveSubject(dispatch, messageKeys) {
    if (messageKeys.length === 1) {
        const messages = await dispatch("$_getIfNotPresent", messageKeys);
        if (messages[0]) {
            return messages[0].subject;
        } else {
            // FIXME : need spec here --> what do we want to display when removing a draft ?
            return "My draft";
        }
    }
}

function doAlert(action, type, commit, messageKeys, subject, uid) {
    let alert;
    if (messageKeys.length > 1) {
        alert = {
            code: "MSG_MULTIPLE_" + action + "_" + type,
            uid
        };
    } else {
        alert = {
            code: "MSG_" + action + "_" + type,
            uid,
            props: { subject }
        };
    }

    commit("addApplicationAlert", alert, { root: true });
}

function loadingAlert(action, commit, messageKeys, subject) {
    const loadingAlertUid = UUIDGenerator.generate();
    doAlert(action, "LOADING", commit, messageKeys, subject, loadingAlertUid);
    return loadingAlertUid;
}

function okAlert(action, commit, messageKeys, subject) {
    doAlert(action, "OK", commit, messageKeys, subject);
}

function errorAlert(action, commit, messageKeys, subject) {
    doAlert(action, "ERROR", commit, messageKeys, subject, null);
}

function allMessagesInTrash(rootGetters, messageKeys) {
    messageKeys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    return messageKeys.every(key => ItemUri.container(key) === rootGetters["mail/MY_TRASH"].key);
}
