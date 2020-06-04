export function getAttachmentProgress(state) {
    return uid => state.attachmentProgresses[uid];
}
