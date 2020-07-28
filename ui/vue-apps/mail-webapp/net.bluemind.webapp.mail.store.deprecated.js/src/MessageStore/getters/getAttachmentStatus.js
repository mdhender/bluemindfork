export function getAttachmentStatus(state) {
    return uid => state.attachmentStatuses[uid];
}
