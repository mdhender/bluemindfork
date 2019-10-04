import ServiceLocator from "@bluemind/inject";

export function loadUnreadCount({ commit }, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistance")
        .get(folderUid)
        .getPerUserUnread()
        .then(count => commit("setUnreadCount", { folderUid, count: count.total }));
}
