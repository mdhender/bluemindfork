import initialState from "../state";

export function clear(state) {
    Object.assign(state, initialState());
}
