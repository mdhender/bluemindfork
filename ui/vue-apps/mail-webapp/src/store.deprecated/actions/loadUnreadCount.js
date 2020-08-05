import ServiceLocator from "@bluemind/inject";
import { SET_UNREAD_COUNT } from "../../store/";

export function loadUnreadCount({ commit }, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .getPerUserUnread()
        .then(count => commit(SET_UNREAD_COUNT, { key: folderUid, count: count.total }, { root: true }));
}
