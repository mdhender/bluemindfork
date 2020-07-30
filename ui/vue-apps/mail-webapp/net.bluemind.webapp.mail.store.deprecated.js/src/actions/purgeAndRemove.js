import { SET_UNREAD_COUNT } from "@bluemind/webapp.mail.store";
import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

export function purge(context, messageKeys) {
    return action(context, messageKeys, "PURGE", purgeMessages);
}

export function remove(context, messageKeys) {
    let actionFunction = removeMessages;
    let type = "REMOVED";
    if (allMessagesInTrash(context.getters, messageKeys)) {
        actionFunction = purgeMessages;
        type = "PURGE";
    }
    return action(context, messageKeys, type, actionFunction);
}

async function action(context, messageKeys, action, actionFunction) {
    messageKeys = [...(Array.isArray(messageKeys) ? messageKeys : [messageKeys])];
    const subject = await retrieveSubject(context.dispatch, messageKeys);
    const loadingAlertUid = loadingAlert(action, context.commit, messageKeys, subject);
    try {
        const messages = await context.dispatch("$_getIfNotPresent", messageKeys);
        const unreadMessageKeys = messages.filter(m => m.states.includes("not-seen")).map(m => m.key);
        cleanUp(messageKeys, unreadMessageKeys, context);
        await actionFunction(context, messageKeys);
        okAlert(action, context.commit, messageKeys, subject);
    } catch (e) {
        errorAlert(action, context.commit, messageKeys, subject);
    } finally {
        context.commit("removeApplicationAlert", loadingAlertUid, { root: true });
    }
}

function purgeMessages(context, messageKeys) {
    return context.dispatch("messages/remove", messageKeys);
}

function removeMessages(context, messageKeys) {
    return context.dispatch("$_move", {
        messageKeys: messageKeys,
        destinationKey: context.getters.my.TRASH.key
    });
}

function cleanUp(messageKeys, unreadMessageKeys, context) {
    const messageKeysByFolder = ItemUri.urisByContainer(messageKeys);
    Object.keys(messageKeysByFolder).forEach(folderUid => {
        const keys = messageKeysByFolder[folderUid];
        keys.forEach(messageKey => {
            if (unreadMessageKeys.includes(messageKey)) {
                context.commit(
                    SET_UNREAD_COUNT,
                    {
                        key: folderUid,
                        count: context.rootState.mail.folders[folderUid].unread - 1
                    },
                    { root: true }
                );
            }
            context.commit("deleteSelectedMessageKey", messageKey);
            if (context.state.currentMessage.key === messageKey) {
                context.state.currentMessage.key = null;
            }
        });
    });
}

function retrieveSubject(dispatch, messageKeys) {
    if (messageKeys.length === 1) {
        return dispatch("$_getIfNotPresent", messageKeys).then(messages => messages[0].subject);
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

function allMessagesInTrash(getters, messageKeys) {
    messageKeys = Array.isArray(messageKeys) ? messageKeys : [messageKeys];
    return messageKeys.every(key => ItemUri.container(key) === getters.my.TRASH.uid);
}
