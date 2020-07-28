import { state } from "../../src/ContainersStore/state";

describe("[ContainersStore][state]", () => {
    test("", () => {
        expect(state).toMatchSnapshot();
    });
});
