export function setCurrentMessageParts(state, { attachments, inlines }) {
    Object.assign(state.currentMessageParts, { attachments, inlines });
}
