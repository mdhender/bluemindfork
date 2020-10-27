import * as state from "../state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchInlineSnapshot(`
            Object {
              "maxMessageSize": undefined,
              "messagesWithUnblockedRemoteImages": Array [],
              "showBlockedImagesAlert": false,
              "status": "idle",
              "userUid": undefined,
            }
        `);
    });
});
