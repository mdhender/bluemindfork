import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";

// TODO Command pattern // Handle errors and undo
export function markAsUnread({ dispatch, state, commit }, messageKey) {
    const folderUid = ItemUri.container(messageKey);
    return dispatch("$_getIfNotPresent", messageKey).then(message => {
        if (!message.states.includes("not-seen")) {
            commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread + 1 });
            return dispatch("messages/deleteFlag", { messageKey, mailboxItemFlag: Flag.SEEN });
        }
    });
}
