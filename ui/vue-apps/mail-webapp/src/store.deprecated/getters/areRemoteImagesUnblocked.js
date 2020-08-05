export function areRemoteImagesUnblocked(state) {
    return messageKey => state.messagesWithUnblockedRemoteImages.includes(messageKey);
}
