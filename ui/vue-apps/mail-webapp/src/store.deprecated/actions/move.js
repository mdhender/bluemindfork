import { Flag } from "@bluemind/email";
import UUIDGenerator from "@bluemind/uuid";
import { MailboxType } from "../../store/helpers/MailboxAdaptor";
import { MESSAGE_IS_LOADED, MY_MAILBOX_KEY } from "~getters";
import { ItemUri } from "@bluemind/item-uri";

export function move(context, { messageKey, folder }) {
    const isArray = Array.isArray(messageKey);
    if (isArray && messageKey.length > 1) {
        return moveMultipleMessages(context, { messageKeys: messageKey, folder });
    } else {
        messageKey = isArray ? messageKey[0] : messageKey;
        return moveSingleMessage(context, { messageKey, folder });
    }
}

function moveSingleMessage({ dispatch, commit, rootState, rootGetters }, { messageKey, folder }) {
    let subject, destination, source, isDestinationMailshare, isUnread;
    return dispatch("$_getIfNotPresent", [messageKey])
        .then(messages => {
            source = rootState.mail.folders[messages[0].folderRef.key];
            subject = messages[0].subject;
            isUnread = !messages[0].flags.includes(Flag.SEEN);
            return dispatch("$_createFolder", { folder, mailboxUid: rootGetters["mail/" + MY_MAILBOX_KEY] });
        })
        .then(key => {
            destination = rootState.mail.folders[key];
            isDestinationMailshare =
                rootState.mail.mailboxes[destination.mailboxRef.key].type === MailboxType.MAILSHARE;
            return dispatch("$_move", { messageKeys: [messageKey], destinationKey: key });
        })
        .then(() => updateUnreadCount(rootState, commit, isUnread, destination, source))
        .then(() => addOkAlert(commit, subject, destination, isDestinationMailshare))
        .catch(error => addErrorAlert(commit, subject, folder, error));
}

function updateUnreadCount(rootState, commit, isUnread, destination, source) {
    if (isUnread) {
        commit("mail/SET_UNREAD_COUNT", { key: destination.key, count: destination.unread + 1 }, { root: true });
        commit("mail/SET_UNREAD_COUNT", { key: source.key, count: source.unread - 1 }, { root: true });
    }
}

async function moveMultipleMessages(context, { messageKeys, folder }) {
    const { dispatch, commit, rootState, rootGetters } = context;
    messageKeys = [...messageKeys];
    const unreadCountInfo = await computeUnreadCountInfo(context, messageKeys);
    let destination, isDestinationMailshare;
    const alertUid = UUIDGenerator.generate();
    addLoadingAlertForMultipleMessages(commit, messageKeys.length, alertUid);
    return dispatch("$_createFolder", { folder, mailboxUid: rootGetters["mail/" + MY_MAILBOX_KEY] })
        .then(key => {
            destination = rootState.mail.folders[key];
            isDestinationMailshare =
                rootState.mail.mailboxes[destination.mailboxRef.key].type === MailboxType.MAILSHARE;

            return dispatch("$_move", { messageKeys, destinationKey: key });
        })
        .then(() => updateUnreadCountMultiple(context, destination, unreadCountInfo))
        .then(() => addOkAlertForMultipleMessages(commit, messageKeys.length, destination, isDestinationMailshare))
        .catch(() => addErrorAlertForMultipleMessages(commit, folder))
        .finally(() => commit("alert/removeApplicationAlert", alertUid, { root: true }));
}

async function computeUnreadCountInfo(context, messageKeys) {
    const { rootGetters, rootState } = context;
    const allLoaded = messageKeys.every(messageKey => rootGetters["mail/" + MESSAGE_IS_LOADED](messageKey));
    let unreadCountInfo;
    if (allLoaded) {
        // gather necessary info to commit the mutation SET_UNREAD_COUNT after move
        unreadCountInfo = { allLoaded: true, total: 0, perFolder: {} };
        messageKeys.forEach(key => {
            const message = rootState.mail.messages[key];
            const isUnread = !message.flags.includes(Flag.SEEN);
            if (isUnread) {
                const folderCount = unreadCountInfo.perFolder[message.folderRef.key];
                unreadCountInfo.perFolder[message.folderRef.key] = folderCount ? folderCount + 1 : 1;
                unreadCountInfo.total++;
            }
        });
    } else {
        // gather necessary info to dispatch the action 'loadUnreadCount' after move
        const sourceFolders = Object.keys(ItemUri.urisByContainer(messageKeys));
        unreadCountInfo = { allLoaded: false, sourceFolders };
    }
    return unreadCountInfo;
}

async function updateUnreadCountMultiple(context, destination, unreadCountInfo) {
    const { dispatch, rootState, commit } = context;
    if (unreadCountInfo.allLoaded && unreadCountInfo.total) {
        commit(
            "mail/SET_UNREAD_COUNT",
            { key: destination.key, count: destination.unread + unreadCountInfo.total },
            { root: true }
        );

        Object.entries(unreadCountInfo.perFolder).forEach(entry => {
            const folderKey = entry[0];
            const count = entry[1];
            const folder = rootState.mail.folders[folderKey];
            commit("mail/SET_UNREAD_COUNT", { key: folderKey, count: folder.unread - count }, { root: true });
        });
    } else if (!unreadCountInfo.allLoaded) {
        dispatch("loadUnreadCount", destination.key);
        unreadCountInfo.sourceFolders.forEach(sourceFolder => dispatch("loadUnreadCount", sourceFolder));
    }
}

function addErrorAlert(commit, subject, folder, error) {
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_ERROR",
            props: { subject, folderName: folder.name, reason: error.message }
        },
        { root: true }
    );
}

function addErrorAlertForMultipleMessages(commit, folder) {
    commit(
        "alert/addApplicationAlert",
        { code: "MSG_MOVE_ERROR_MULTIPLE", props: { folderName: folder.name } },
        { root: true }
    );
}

function addOkAlert(commit, subject, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.path } : { folder: folder.path };
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_OK",
            props: {
                subject,
                folder,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addOkAlertForMultipleMessages(commit, count, folder, isMailshare) {
    const params = isMailshare ? { mailshare: folder.path } : { folder: folder.path };
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVE_OK_MULTIPLE",
            props: {
                count,
                folder,
                folderNameLink: { name: "v:mail:home", params }
            }
        },
        { root: true }
    );
}

function addLoadingAlertForMultipleMessages(commit, count, alertUid) {
    commit(
        "alert/addApplicationAlert",
        {
            code: "MSG_MOVED_LOADING_MULTIPLE",
            props: { count },
            uid: alertUid
        },
        { root: true }
    );
}
