export function updateSeen(state, { id, isSeen }) {
    const flags = state.items[id].value.systemFlags;
    if (flags.includes("seen") != isSeen) {
        if (isSeen) {
            flags.push("seen");
        } else {
            flags.splice(flags.indexOf("seen"), 1);
        }
    }
}
