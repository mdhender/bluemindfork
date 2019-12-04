export function containers(state) {
    return state.containerKeys.map(key => state.containers[key]);
}
