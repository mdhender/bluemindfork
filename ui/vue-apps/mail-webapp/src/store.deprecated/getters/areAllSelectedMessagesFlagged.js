import { Flag } from "@bluemind/email";

export function areAllSelectedMessagesFlagged(state, getters) {
    /*
     * BEST EFFORT
     *    We consider only already fetched messages (for performance purpose).
     */
    return state.selectedMessageKeys.every(selectedMessageKey => {
        const selectedItem = getters["messages/getMessageByKey"](selectedMessageKey);
        return !selectedItem || selectedItem.flags.includes(Flag.FLAGGED);
    });
}
