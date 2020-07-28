import { Flag } from "@bluemind/email";

export function areAllSelectedMessagesUnread(state) {
    /*
     * BEST EFFORT
     *    We consider only already fetched messages (for performance purpose).
     */
    return state.selectedMessageKeys.every(selectedMessageKey => {
        const selectedItem = state.messages.items[selectedMessageKey];
        return !selectedItem || !selectedItem.value.flags.includes(Flag.SEEN);
    });
}
