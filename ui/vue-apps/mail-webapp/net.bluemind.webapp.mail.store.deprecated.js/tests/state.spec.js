import * as state from "../src/state";

describe("[Mail-WebappStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchInlineSnapshot(`
            Object {
              "currentFolderKey": undefined,
              "foldersData": Object {},
              "maxMessageSize": undefined,
              "messageFilter": undefined,
              "messagesWithUnblockedRemoteImages": Array [],
              "selectedMessageKeys": Array [],
              "showBlockedImagesAlert": false,
              "status": "idle",
              "userSettings": Object {},
              "userUid": undefined,
            }
        `);
    });
});
