export function addFlag(state, { messageKey, mailboxItemFlag }) {
    let flags = state.items[messageKey].value.flags;
    if (flags.find(flag => flag === mailboxItemFlag) === undefined) {
        state.items[messageKey].value.flags.push(mailboxItemFlag);
    }
}
