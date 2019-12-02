export function getFoldersByMailbox(state, getters) {
    return mailbox => (state.itemsByContainer[mailbox] || []).map(key => getters.getFolderByKey(key));
}
