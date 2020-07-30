import ServiceLocator from "@bluemind/inject";
import { SET_UNREAD_COUNT } from "@bluemind/webapp.mail.store";

export function loadUnreadCount({ commit }, folderUid) {
    console.log("OLA unread count !!");
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .getPerUserUnread()
        .then(count => commit(SET_UNREAD_COUNT, { key: folderUid, count: count.total }, { root: true }));
}
