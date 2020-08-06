import ServiceLocator from "@bluemind/inject";

export function loadUnreadCount({ commit }, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .getPerUserUnread()
        .then(count => commit("mail/SET_UNREAD_COUNT", { key: folderUid, count: count.total }, { root: true }));
}
