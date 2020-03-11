import * as messageListMode from "../../src/getters/messageListMode";

describe("[Mail-WebappStore]", () => {
    const { isSearchMode, isFolderMode } = messageListMode;
    describe("getters", () => {
        test("search mode is active state", () => {
            const state = {
                search: {
                    pattern: "foo"
                }
            };
            expect(isSearchMode(state)).toBeTruthy();
            expect(isFolderMode(state)).toBeFalsy();
        });
        test("idle status active folder mode", () => {
            const state = {
                search: {
                    status: "idle"
                }
            };
            expect(isSearchMode(state)).toBeFalsy();
            expect(isFolderMode(state)).toBeTruthy();
        });
    });
});
