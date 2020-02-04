export function removeAttachment(state, attachmentUid) {
    const attachmentIndex = state.parts.attachments.findIndex(a => a.uid === attachmentUid);
    state.parts.attachments.splice(attachmentIndex, 1);
}
