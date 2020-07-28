export function unblockRemoteImages(state, messageKey) {
    state.messagesWithUnblockedRemoteImages.push(messageKey);
}
