export function addFlag(state, { messageKey, mailboxItemFlag }) {
    let flags = state.items[messageKey].value.flags;
    if (flags.map(f => f.flag).find(f => f === mailboxItemFlag.flag) === undefined) {
        state.items[messageKey].value.flags.push(mailboxItemFlag);
    }
}
