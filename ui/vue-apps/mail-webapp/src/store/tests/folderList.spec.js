import cloneDeep from "lodash.clonedeep";
import Vue from "vue";
import Vuex from "vuex";
import {
    RESET_FILTER_LIMITS,
    SET_FILTER_PATTERN,
    SET_FILTER_RESULTS,
    SET_FILTER_LIMIT,
    SET_FILTER_STATUS,
    TOGGLE_EDIT_FOLDER
} from "~/mutations";
import { FOLDER_LIST_IS_EMPTY, FOLDER_LIST_IS_FILTERED, FOLDER_LIST_IS_LOADING, FOLDER_LIST_RESULTS } from "~/getters";
import { default as initialStore, FolderListStatus } from "../folderList";

Vue.use(Vuex);
let store;

describe("folderList store", () => {
    beforeEach(() => {
        store = cloneDeep(initialStore);
    });
    describe("mutations", () => {
        test("RESET_FILTER_LIMITS", () => {
            store.state.limits = "truc";
            store.mutations[RESET_FILTER_LIMITS](store.state);
            expect(store.state.limits).toEqual({});
        });
        test("SET_FILTER_PATTERN", () => {
            store.mutations[SET_FILTER_PATTERN](store.state, "bla");
            expect(store.state.pattern).toEqual("bla");

            store.mutations[SET_FILTER_PATTERN](store.state, "");
            expect(store.state.pattern).toEqual("");
        });
        test("SET_FILTER_RESULTS", () => {
            expect(store.state.results).toEqual({});

            store.mutations[SET_FILTER_RESULTS](store.state, "truc");
            expect(store.state.results).toEqual("truc");
        });
        test("SET_FILTER_LIMIT", () => {
            expect(store.state.limits).toEqual({});

            store.mutations[SET_FILTER_LIMIT](store.state, { mailbox: { key: "truc" }, limits: "machin" });
            expect(store.state.limits).toEqual({ truc: "machin" });
        });
        test("SET_FILTER_STATUS", () => {
            expect(store.state.status).toEqual(FolderListStatus.IDLE);

            store.mutations[SET_FILTER_STATUS](store.state, "truc");
            expect(store.state.status).toEqual("truc");
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
    });
    describe("getters", () => {
        test("FOLDER_LIST_IS_EMPTY", () => {
            expect(store.getters[FOLDER_LIST_IS_EMPTY](store.state)).toBeTruthy();
            store.state.results["machin"] = [];
            expect(store.getters[FOLDER_LIST_IS_EMPTY](store.state)).toBeTruthy();
            store.state.results["machin"] = ["a", "b", "c"];
            expect(store.getters[FOLDER_LIST_IS_EMPTY](store.state)).toBeFalsy();
            store.state.results["machin"] = [];
            store.state.results["truc"] = ["a", "b", "c"];
            store.state.results["bidule"] = undefined;
            expect(store.getters[FOLDER_LIST_IS_EMPTY](store.state)).toBeFalsy();
        });
        test("FOLDER_LIST_IS_FILTERED", () => {
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state)).toBeFalsy();
            store.state.pattern = "blabla";
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state)).toBeTruthy();
            store.state.pattern = "";
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state)).toBeFalsy();
            store.state.pattern = null;
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state)).toBeFalsy();
            store.state.pattern = undefined;
            expect(store.getters[FOLDER_LIST_IS_FILTERED](store.state)).toBeFalsy();
        });
        test("FOLDER_LIST_IS_LOADING", () => {
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
            store.state.status = "plop";
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
            store.state.status = FolderListStatus.LOADING;
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeTruthy();
        });
        test("FOLDER_LIST_RESULTS", () => {
            expect(store.getters[FOLDER_LIST_RESULTS](store.state)({ key: "machin" })).toBeFalsy();
            store.state.results["machin"] = "truc";
            expect(store.getters[FOLDER_LIST_RESULTS](store.state)({ key: "machin" })).toEqual("truc");
        });
    });
    describe("actions", () => {
        test.skip("FILTER_FOLDERS", () => {
            // TODO
        });
    });
});
