import Vue from "vue";

export function storeContainers(state, containers) {
    containers.forEach(container => {
        if (!state.containerKeys.includes(container.uid)) {
            state.containerKeys.push(container.uid);
        }
        Vue.set(state.containers, container.uid, container);
    });
}
