import ServiceLocator from "@bluemind/inject";

export function all({ commit }, { type, verb }) {
    return ServiceLocator.getProvider("ContainersPersistence")
        .get()
        .all({ type, verb })
        .then(containers => commit("storeContainers", containers));
}
