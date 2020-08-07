import { isSameSearch } from "../../getters/isSameSearch";

let state;

describe("[Mail-WebappStore][getters] : isSameSearch", () => {
    beforeEach(() => {
        state = {
            search: {
                pattern: "mybeautifulpattern",
                searchFolder: "myawesomefolder"
            }
        };
    });
    test("Same search 1", async () => {
        expect(isSameSearch(state)("mybeautifulpattern", "myawesomefolder")).toBeTruthy();
    });
    test("Same search 2", async () => {
        state.search.searchFolder = undefined;
        expect(isSameSearch(state)("mybeautifulpattern", undefined)).toBeTruthy();
    });
    test("Same search 3", async () => {
        state.search.searchFolder = undefined;
        expect(isSameSearch(state)("mybeautifulpattern", null)).toBeTruthy();
    });
    test("Same search 4", async () => {
        state.search.searchFolder = undefined;
        expect(isSameSearch(state)("mybeautifulpattern", "")).toBeTruthy();
    });
    test("Not same search 1", async () => {
        expect(isSameSearch(state)("mybeautifulpattern plop", "myawesomefolder")).toBeFalsy();
    });
    test("Not same search 2", async () => {
        expect(isSameSearch(state)("mybeautifulpattern", "myaweso mefolder")).toBeFalsy();
    });
    test("Not same search 3", async () => {
        expect(isSameSearch(state)("mybeautifulpattern", null)).toBeFalsy();
    });
    test("Not same search 4", async () => {
        expect(isSameSearch(state)(undefined, "myawesomefolder")).toBeFalsy();
    });
});
