export function currentMessageAttachments(state, getters) {
    if (state.currentMessageKey) {
        return state.currentMessageParts.attachments.map(part =>
            Object.assign({}, part, {
                content: getters["messages/getPartContent"](state.currentMessageKey, part.address)
            })
        );
    }
    return [];
}
