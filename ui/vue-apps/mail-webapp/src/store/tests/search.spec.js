import cloneDeep from "lodash.clonedeep";
import store from "../search";
import { CONVERSATION_LIST_IS_FILTERED, HAS_PATTERN, IS_TYPING_IN_SEARCH } from "~/getters";
import {
    SET_CURRENT_SEARCH_PATTERN,
    SET_SEARCH_QUERY_FOLDER,
    SET_CURRENT_SEARCH_FOLDER,
    SET_SEARCH_QUERY_PATTERN,
    SET_CURRENT_SEARCH_DEEP,
    SET_SEARCH_QUERY_DEEP
} from "~/mutations";

describe("search", () => {
    let state;
    beforeEach(() => {
        state = cloneDeep(store.state);
    });
    describe("mutations", () => {
        test("SET_SEARCH_QUERY_PATTERN", () => {
            const pattern = "Search pattern";
            store.mutations[SET_SEARCH_QUERY_PATTERN](state, pattern);
            expect(state.searchQuery.pattern).toEqual(pattern);
        });
        test("SET_CURRENT_SEARCH_PATTERN", () => {
            const pattern = "Search pattern";
            store.mutations[SET_CURRENT_SEARCH_PATTERN](state, pattern);
            expect(state.currentSearch.pattern).toEqual(pattern);
        });
        test("SET_SEARCH_QUERY_FOLDER", () => {
            const folder = "folder...";
            store.mutations[SET_SEARCH_QUERY_FOLDER](state, folder);
            expect(state.searchQuery.folder).toEqual(folder);
        });
        test("SET_CURRENT_SEARCH_FOLDER", () => {
            const folder = "folder...";
            store.mutations[SET_CURRENT_SEARCH_FOLDER](state, folder);
            expect(state.currentSearch.folder).toEqual(folder);
        });
        test("SET_SEARCH_QUERY_DEEP", () => {
            expect(state.searchQuery.deep).toEqual(true);
            const deep = false;
            store.mutations[SET_SEARCH_QUERY_DEEP](state, deep);
            expect(state.searchQuery.deep).toEqual(deep);
        });
        test("SET_CURRENT_SEARCH_DEEP", () => {
            expect(state.currentSearch.deep).toEqual(true);
            const deep = false;
            store.mutations[SET_CURRENT_SEARCH_DEEP](state, deep);
            expect(state.currentSearch.deep).toEqual(deep);
        });
    });
    describe("getters", () => {
        test("CONVERSATION_LIST_IS_FILTERED", () => {
            expect(store.getters[CONVERSATION_LIST_IS_FILTERED](state)).toBeFalsy();
            state.searchQuery.pattern = "Search pattern";
            expect(store.getters[CONVERSATION_LIST_IS_FILTERED](state)).toBeTruthy();
            state.searchQuery.pattern = "   ";
            expect(store.getters[CONVERSATION_LIST_IS_FILTERED](state)).toBeFalsy();
        });
        test("HAS_PATTERN", () => {
            expect(store.getters[HAS_PATTERN](state)).toBeFalsy();
            state.currentSearch.pattern = "Search pattern";
            expect(store.getters[HAS_PATTERN](state)).toBeTruthy();
            state.currentSearch.pattern = "";
            expect(store.getters[HAS_PATTERN](state)).toBeTruthy();
        });
        test("IS_TYPING_IN_SEARCH", () => {
            expect(store.getters[IS_TYPING_IN_SEARCH](state)).toBeFalsy();
            state.currentSearch.pattern = "Search pattern";
            expect(store.getters[IS_TYPING_IN_SEARCH](state)).toBeTruthy();
            state.searchQuery.pattern = "Search pattern";
            expect(store.getters[IS_TYPING_IN_SEARCH](state)).toBeFalsy();
        });
    });
});
