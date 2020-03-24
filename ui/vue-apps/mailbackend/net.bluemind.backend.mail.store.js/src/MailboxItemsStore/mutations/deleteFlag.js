export function deleteFlag(state, { messageKey, mailboxItemFlag }) {
    let flags = state.items[messageKey].value.flags;
    const indexToRemove = flags.findIndex(flag => flag === mailboxItemFlag);
    if (indexToRemove !== -1) {
        state.items[messageKey].value.flags.splice(indexToRemove, 1);
    }
}
