import ServiceLocator from "@bluemind/inject";
import { SET_UNREAD_COUNT } from "~mutations";

export function loadUnreadCount({ commit }, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .getPerUserUnread()
        .then(count => commit("mail/" + SET_UNREAD_COUNT, { key: folderUid, unread: count.total }, { root: true }));
}
