import * as state from "../state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchInlineSnapshot(`
            Object {
              "maxMessageSize": undefined,
              "messageFilter": undefined,
              "messagesWithUnblockedRemoteImages": Array [],
              "selectedMessageKeys": Array [],
              "showBlockedImagesAlert": false,
              "status": "idle",
              "userUid": undefined,
            }
        `);
    });
});
