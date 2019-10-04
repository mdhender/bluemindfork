import Vue from "vue";

export function storePart(state, { id, address, content }) {
    if (!state.parts[id]) {
        Vue.set(state.parts, id, { [address]: content });
    } else {
        Vue.set(state.parts[id], address, content);
    }
}
