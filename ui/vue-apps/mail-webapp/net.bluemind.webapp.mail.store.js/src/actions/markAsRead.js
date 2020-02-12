import { Flag } from "@bluemind/email";
import ItemUri from "@bluemind/item-uri";

export function markAsRead({ dispatch, state, commit }, messageKey) {
    const folderUid = ItemUri.container(messageKey);
    return dispatch("$_getIfNotPresent", messageKey).then(message => {
        if (message.states.includes("not-seen")) {
            commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            return dispatch("messages/addFlag", { messageKey, mailboxItemFlag: Flag.SEEN });
        }
    });
}
