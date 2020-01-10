import { storeContainers } from "../../../src/ContainersStore/mutations/storeContainers";

jest.mock("@bluemind/inject");

describe("[ContainersStore][mutations] : storeContainers", () => {
    test("add containers not already present in state, update the others", () => {
        const state = { containerKeys: [], containers: {} };
        const containers = [
            { uid: "container-1", value: { name: "Container 1" } },
            { uid: "container-2", value: { name: "Container 2" } },
            { uid: "container-3", value: { name: "Container 3" } }
        ];
        storeContainers(state, containers);
        const update = [
            { uid: "container-2", value: { name: "Container 2 Updated" } },
            { uid: "container-4", value: { name: "Container 4" } }
        ];
        storeContainers(state, update);
        let result = { containers: {}, containerKeys: [] };
        containers.concat(update).forEach(container => {
            result.containers[container.uid] = container;
            if (!result.containerKeys.includes(container.uid)) {
                result.containerKeys.push(container.uid);
            }
        });
        expect(state).toEqual(result);
    });
    test("update only the needed part of the state", () => {
        const state = { containerKeys: [], containers: {} };
        const containers = [{ uid: "container-1", value: { name: "Container 1" } }];
        storeContainers(state, containers);
        expect(Object.keys(state)).toEqual(["containerKeys", "containers"]);
    });
});
