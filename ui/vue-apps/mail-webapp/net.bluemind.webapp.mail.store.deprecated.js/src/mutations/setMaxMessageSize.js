export function setMaxMessageSize(state, maxSize) {
    // take into account the email base64 encoding : 33% more space
    state.maxMessageSize = maxSize / 1.33;
}
