import mutationTypes from "../mutationTypes";
import cloneDeep from "lodash.clonedeep";
import store from "../search";

describe("search", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_SEARCH_PATTERN", () => {
            const pattern = "Search pattern";
            store.mutations[mutationTypes.SET_SEARCH_PATTERN](state, pattern);
            expect(state.pattern).toEqual(pattern);
        });
        test("SET_SEARCH_FOLDER", () => {
            const folder = "folder...";
            store.mutations[mutationTypes.SET_SEARCH_FOLDER](state, folder);
            expect(state.folder).toEqual(folder);
        });
    });
    describe("getters", () => {
        test("MESSAGE_LIST_IS_SEARCH_MODE", () => {
            expect(store.getters.MESSAGE_LIST_IS_SEARCH_MODE(state)).toBeFalsy();
            state.pattern = "Search pattern";
            expect(store.getters.MESSAGE_LIST_IS_SEARCH_MODE(state)).toBeTruthy();
            state.pattern = "   ";
            expect(store.getters.MESSAGE_LIST_IS_SEARCH_MODE(state)).toBeFalsy();
        });
    });
});
