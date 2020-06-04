import * as state from "../src/state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchSnapshot();
    });
});
