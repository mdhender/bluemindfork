export function addFlag(state, { messageKeys, mailboxItemFlag }) {
    messageKeys.forEach(messageKey => {
        const message = state.items[messageKey];
        if (message) {
            let flags = message.value.flags;
            if (flags.find(flag => flag === mailboxItemFlag) === undefined) {
                message.value.flags.push(mailboxItemFlag);
            }
        }
    });
}
