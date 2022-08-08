import cloneDeep from "lodash.clonedeep";
import Vue from "vue";
import Vuex from "vuex";
import { RESET_FILTER, SHOW_MORE_FOR_GROUP_MAILBOXES, SHOW_MORE_FOR_MAILSHARES, SHOW_MORE_FOR_USERS } from "~/actions";
import {
    RESET_FOLDER_FILTER_LIMITS,
    SET_FOLDER_FILTER_PATTERN,
    SET_FOLDER_FILTER_RESULTS,
    SET_FOLDER_FILTER_LIMIT,
    SET_FOLDER_FILTER_LOADED,
    SET_FOLDER_FILTER_LOADING,
    TOGGLE_EDIT_FOLDER,
    SET_FOLDER_EXPANDED,
    SET_COLLAPSED_TREE
} from "~/mutations";
import {
    FOLDER_LIST_IS_FILTERED,
    FOLDER_LIST_IS_LOADING,
    FOLDER_LIST_LIMIT_FOR_GROUP_MAILBOX,
    FOLDER_LIST_LIMIT_FOR_MAILSHARE,
    FOLDER_LIST_LIMIT_FOR_USER
} from "~/getters";
import { default as initialStore, DEFAULT_LIMIT } from "../folderList";

Vue.use(Vuex);
let store;
let activeStore;

describe("folderList store", () => {
    beforeEach(() => {
        store = cloneDeep(initialStore);
        activeStore = new Vuex.Store(store);
    });

    describe("state", () => {
        test("collapsed folderTrees and expandedFolders are synced with app data", async () => {
            const syncedKeys = Object.keys(store.state.synced);
            expect(["expandedFolders", "collapsedTrees"].every(appData => syncedKeys.includes(appData))).toEqual(true);
        });
    });

    describe("mutations", () => {
        test("RESET_FOLDER_FILTER_LIMITS", () => {
            store.state.limits = "truc";
            store.mutations[RESET_FOLDER_FILTER_LIMITS](store.state);
            expect(store.state.limits).toEqual({});
        });
        test("SET_FOLDER_FILTER_PATTERN", () => {
            store.mutations[SET_FOLDER_FILTER_PATTERN](store.state, "bla");
            expect(store.state.pattern).toEqual("bla");

            store.mutations[SET_FOLDER_FILTER_PATTERN](store.state, "");
            expect(store.state.pattern).toEqual("");
        });
        test("SET_FOLDER_FILTER_RESULTS", () => {
            expect(store.state.results).toEqual({});

            store.mutations[SET_FOLDER_FILTER_RESULTS](store.state, "truc");
            expect(store.state.results).toEqual("truc");
        });
        test("SET_FOLDER_FILTER_LIMIT", () => {
            expect(store.state.limits).toEqual({});

            store.mutations[SET_FOLDER_FILTER_LIMIT](store.state, { mailbox: { key: "truc" }, limit: "machin" });
            expect(store.state.limits).toEqual({ truc: "machin" });
        });
        test("SET_FOLDER_FILTER_LOADING", () => {
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();

            store.mutations[SET_FOLDER_FILTER_LOADING](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeTruthy();
        });
        test("SET_FOLDER_FILTER_LOADED", () => {
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();

            store.mutations[SET_FOLDER_FILTER_LOADING](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeTruthy();

            store.mutations[SET_FOLDER_FILTER_LOADED](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
        });
        test("TOGGLE_EDIT_FOLDER: define the folder to be edited", () => {
            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            expect(store.state.editing).toEqual("1");

            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            expect(store.state.editing).toEqual(undefined);

            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "2");
            expect(store.state.editing).toEqual("2");
        });

        test("SET_FOLDER_EXPANDED", () => {
            expect(store.state.expandedFolders).toEqual([]);
            store.mutations[SET_FOLDER_EXPANDED](store.state, { key: "123", expanded: true });
            expect(store.state.expandedFolders).toEqual(["123"]);
            store.mutations[SET_FOLDER_EXPANDED](store.state, { key: "123", expanded: false });
            expect(store.state.expandedFolders).toEqual([]);
        });

        test("SET_COLLAPSED_TREE", () => {
            expect(store.state.collapsedTrees).toEqual([]);
            store.mutations[SET_COLLAPSED_TREE](store.state, { key: "123", collapsed: true });
            expect(store.state.collapsedTrees).toEqual([{ key: "123", collapsed: true }]);
            store.mutations[SET_COLLAPSED_TREE](store.state, { key: "123", collapsed: false });
            expect(store.state.collapsedTrees).toEqual([{ key: "123", collapsed: false }]);
        });
    });
    describe("getters", () => {
        test("FOLDER_LIST_IS_FILTERED", () => {
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, {})).toBeFalsy();
            store.state.pattern = "blabla";
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, {})).toBeTruthy();
            store.state.pattern = "";
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, {})).toBeFalsy();
            store.state.pattern = null;
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, {})).toBeFalsy();
            store.state.pattern = undefined;
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, {})).toBeFalsy();
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state, { FOLDER_LIST_IS_LOADING: true })).toBeTruthy();
        });
        test("FOLDER_LIST_IS_LOADING", () => {
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
            store.mutations[SET_FOLDER_FILTER_LOADING](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeTruthy();
            store.mutations[SET_FOLDER_FILTER_LOADED](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
        });
    });
    describe("actions", () => {
        test("RESET_FILTER", () => {
            activeStore.dispatch(SHOW_MORE_FOR_MAILSHARES);
            activeStore.commit(SET_FOLDER_FILTER_PATTERN, "truc");
            activeStore.commit(SET_FOLDER_FILTER_RESULTS, "bidule");
            activeStore.dispatch(RESET_FILTER);
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_USER]({ key: "machin" })).toEqual(DEFAULT_LIMIT);
            expect(activeStore.state.pattern).toBeFalsy();
            expect(activeStore.state.results).toEqual({});
        });
        test("SHOW_MORE_FOR_USERS", () => {
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_USER]({ key: "machin" })).toEqual(DEFAULT_LIMIT);
            activeStore.dispatch(SHOW_MORE_FOR_USERS, { key: "machin" });
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_USER]({ key: "machin" })).toEqual(
                DEFAULT_LIMIT + DEFAULT_LIMIT
            );
        });
        test("SHOW_MORE_FOR_MAILSHARES", () => {
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_MAILSHARE]).toEqual(DEFAULT_LIMIT);
            activeStore.dispatch(SHOW_MORE_FOR_MAILSHARES);
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_MAILSHARE]).toEqual(DEFAULT_LIMIT + DEFAULT_LIMIT);
        });
        test("SHOW_MORE_FOR_GROUP_MAILBOXES", () => {
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_GROUP_MAILBOX]).toEqual(DEFAULT_LIMIT);
            activeStore.dispatch(SHOW_MORE_FOR_GROUP_MAILBOXES);
            expect(activeStore.getters[FOLDER_LIST_LIMIT_FOR_GROUP_MAILBOX]).toEqual(DEFAULT_LIMIT + DEFAULT_LIMIT);
        });
    });
});
