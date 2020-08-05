export function clearParts(state) {
    state.partContents = {};
    Object.keys(state.itemsParts).forEach(key => (state.itemsParts[key] = []));
}
