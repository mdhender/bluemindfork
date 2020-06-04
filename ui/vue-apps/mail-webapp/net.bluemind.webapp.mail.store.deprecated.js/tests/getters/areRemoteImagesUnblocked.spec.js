import { areRemoteImagesUnblocked } from "../../src/getters/areRemoteImagesUnblocked";

const state = { messagesWithUnblockedRemoteImages: [] };

describe("[Mail-WebappStore][getters] : areRemoteImagesUnblocked", () => {
    test("Basic", () => {
        const key = "myKey";

        let result = areRemoteImagesUnblocked(state)(key);
        expect(result).toBe(false);

        state.messagesWithUnblockedRemoteImages.push(key);

        result = areRemoteImagesUnblocked(state)(key);
        expect(result).toBe(true);
    });
});
