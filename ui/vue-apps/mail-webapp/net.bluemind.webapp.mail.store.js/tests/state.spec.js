import * as state from "../src/state";

describe("[Mail-WebappStore][state] : initial state", () => {
    test("contains an undefined currentFolderKey", () => {
        expect(state.currentFolderKey).toBeUndefined;
    });
    test("contains an undefined login", () => {
        expect(state.login).toBeUndefined;
    });
    test("contains a empty object 'foldersData'", () => {
        expect(state.foldersData).toEqual({});
    });
    test("contains a 'search'", () => {
        expect(state.search).toEqual({
            pattern: null,
            loading: false,
            error: false
        });
    });
    test("contains a 'messageFilter'", () => {
        expect(state.messageFilter).toBeUndefined();
    });
    test("not to contain anything else", () => {
        expect(Object.keys(state).sort()).toEqual(
            ["currentFolderKey", "foldersData", "login", "search", "messageFilter"].sort()
        );
    });
});
