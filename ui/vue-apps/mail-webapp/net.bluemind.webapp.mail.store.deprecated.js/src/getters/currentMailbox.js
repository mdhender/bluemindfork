import { ItemUri } from "@bluemind/item-uri";

export function currentMailbox(state, getters) {
    const uid = ItemUri.container(state.currentFolderKey);
    if (uid === getters.my.mailboxUid) {
        return getters.my;
    }
    return getters.mailshares.find(mailshare => mailshare.mailboxUid === uid);
}
