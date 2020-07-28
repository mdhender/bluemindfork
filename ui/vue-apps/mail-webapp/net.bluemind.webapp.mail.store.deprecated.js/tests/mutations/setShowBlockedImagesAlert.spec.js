import { setShowBlockedImagesAlert } from "../../src/mutations/setShowBlockedImagesAlert";

const state = { showBlockedImagesAlert: false };

describe("[Mail-WebappStore][mutations] : setShowBlockedImagesAlert", () => {
    test("Basic", () => {
        setShowBlockedImagesAlert(state, true);
        expect(state.showBlockedImagesAlert).toBe(true);
        setShowBlockedImagesAlert(state, false);
        expect(state.showBlockedImagesAlert).toBe(false);
    });
});
