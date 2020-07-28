export function updateAttachment(state, attachment) {
    state.parts.attachments.filter(a => a.uid === attachment.uid).forEach(a => Object.assign(a, attachment));
}
