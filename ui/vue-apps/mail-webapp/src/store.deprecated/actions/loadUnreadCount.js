import ServiceLocator from "@bluemind/inject";
import { ItemFlag } from "@bluemind/core.container.api";
import { SET_UNREAD_COUNT } from "~mutations";


export function loadUnreadCount({ commit }, folderUid) {
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folderUid)
        .count({ must: [], mustNot: [ItemFlag.Deleted, ItemFlag.Seen] })
        .then(count => commit("mail/" + SET_UNREAD_COUNT, { key: folderUid, unread: count.total }, { root: true }));
}
