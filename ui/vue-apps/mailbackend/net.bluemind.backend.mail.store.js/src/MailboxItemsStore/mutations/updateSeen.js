export function updateSeen(state, { messageKey, isSeen }) {
    const flags = state.items[messageKey].value.systemFlags;
    if (flags.includes("seen") !== isSeen) {
        if (isSeen) {
            flags.push("seen");
        } else {
            flags.splice(flags.indexOf("seen"), 1);
        }
    }
}
