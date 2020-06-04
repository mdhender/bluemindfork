import { setCurrentFolder } from "../../src/mutations/setCurrentFolder";

describe("[Mail-WebappStore][mutations] : setCurrentFolder ", () => {
    const state = {};
    beforeEach(() => {
        state.currentFolderKey = "key1";
    });
    test(" mutate currentFolderKey", () => {
        setCurrentFolder(state, "key2");
        expect(state.currentFolderKey).toEqual("key2");
    });
    test(" only mutate currentFolderKey ", () => {
        setCurrentFolder(state, "key2");
        expect(Object.keys(state)).toEqual(["currentFolderKey"]);
    });
});
