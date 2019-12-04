import { state } from "../../src/ContainersStore/state";

describe("[ContainersStore][state] : initial state", () => {
    test("contains an empty object 'containers'", () => {
        expect(state.containers).toEqual({});
    });
    test("contains a empty array 'containerKeys'", () => {
        expect(state.containerKeys).toEqual([]);
    });
    test("does not contain anything else", () => {
        expect(Object.keys(state)).toEqual(["containers", "containerKeys"]);
    });
});
