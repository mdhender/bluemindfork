import { Flag } from "@bluemind/email";

export function areAllSelectedMessagesRead(state, getters, rootState, rootGetters) {
    /*
     * BEST EFFORT
     *    We consider only already fetched messages (for performance purpose).
     */
    return state.selectedMessageKeys.every(selectedMessageKey => {
        const selectedItem = rootState.mail.messages[selectedMessageKey];
        return !rootGetters["mail/isLoaded"](selectedMessageKey) || selectedItem.flags.includes(Flag.SEEN);
    });
}
