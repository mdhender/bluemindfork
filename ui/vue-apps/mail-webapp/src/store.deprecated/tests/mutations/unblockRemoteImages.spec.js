import { unblockRemoteImages } from "../../mutations/unblockRemoteImages";

const state = { messagesWithUnblockedRemoteImages: [] };

describe("[Mail-WebappStore][mutations] : unblockRemoteImages", () => {
    test("Basic", () => {
        const key = "myKey";
        unblockRemoteImages(state, key);
        expect(state.messagesWithUnblockedRemoteImages).toContain(key);
    });
});
