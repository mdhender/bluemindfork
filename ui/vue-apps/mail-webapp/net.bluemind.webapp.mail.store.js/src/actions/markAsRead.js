import ItemUri from "@bluemind/item-uri";

export function markAsRead({ dispatch, state, commit }, messageKey) {
    const folderUid = ItemUri.container(messageKey);
    return dispatch("$_getIfNotPresent", messageKey).then(message => {
        if (message.states.includes("not-seen")) {
            commit("setUnreadCount", { folderUid, count: state.foldersData[folderUid].unread - 1 });
            return dispatch("messages/updateSeen", { messageKey, isSeen: true });
        }
    });
    //FIXME:
    // .catch(() => {});
    // .then(() => refresh mail list if there is a unread filter);
}
