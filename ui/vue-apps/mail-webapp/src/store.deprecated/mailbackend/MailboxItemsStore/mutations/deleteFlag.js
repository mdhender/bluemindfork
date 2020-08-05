export function deleteFlag(state, { messageKeys, mailboxItemFlag }) {
    messageKeys.forEach(messageKey => {
        const message = state.items[messageKey];
        if (message) {
            let flags = message.value.flags;
            const indexToRemove = flags.findIndex(flag => flag === mailboxItemFlag);
            if (indexToRemove !== -1) {
                message.value.flags.splice(indexToRemove, 1);
            }
        }
    });
}
