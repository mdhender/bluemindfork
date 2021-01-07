import * as state from "../state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchInlineSnapshot(`
            Object {
              "maxMessageSize": undefined,
              "status": "idle",
              "userUid": undefined,
            }
        `);
    });
});
