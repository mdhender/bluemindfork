import { ItemUri } from "@bluemind/item-uri";

export function currentMailbox(state, getters, rootState) {
    return rootState.mail.mailboxes[ItemUri.container(state.currentFolderKey)];
}
