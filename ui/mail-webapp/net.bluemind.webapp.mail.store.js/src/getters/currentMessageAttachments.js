export function currentMessageAttachments(state) {
    if (state.currentMessageId) {
        const partsContent = state.messages.parts[state.currentMessageId];
        return state.currentMessageParts.attachments.map(part =>
            Object.assign({ content: partsContent && partsContent[part.address] }, part)
        );
    }
    return [];
}
