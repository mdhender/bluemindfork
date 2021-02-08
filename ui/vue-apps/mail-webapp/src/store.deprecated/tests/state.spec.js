import * as state from "../state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchInlineSnapshot(`
            Object {
              "status": "idle",
              "userUid": undefined,
            }
        `);
    });
});
