export function areMessagesFiltered(state) {
    const messageFilter = state.messageFilter;
    return messageFilter && messageFilter !== "all";
}
