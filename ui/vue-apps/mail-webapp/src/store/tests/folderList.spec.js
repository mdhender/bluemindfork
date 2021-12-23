import cloneDeep from "lodash.clonedeep";
import Vue from "vue";
import Vuex from "vuex";
import {
    RESET_FOLDER_FILTER_LIMITS,
    SET_FOLDER_FILTER_PATTERN,
    SET_FOLDER_FILTER_RESULTS,
    SET_FOLDER_FILTER_LIMIT,
    SET_FOLDER_FILTER_LOADED,
    SET_FOLDER_FILTER_LOADING,
    TOGGLE_EDIT_FOLDER
} from "~/mutations";
import { FOLDER_LIST_IS_FILTERED, FOLDER_LIST_IS_LOADING } from "~/getters";
import { default as initialStore } from "../folderList";

Vue.use(Vuex);
let store;

describe("folderList store", () => {
    beforeEach(() => {
        store = cloneDeep(initialStore);
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

            store.mutations[SET_FOLDER_FILTER_LIMIT](store.state, { mailbox: { key: "truc" }, limits: "machin" });
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
    });
    describe("getters", () => {
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
            store.mutations[SET_FOLDER_FILTER_LOADING](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeTruthy();
            store.mutations[SET_FOLDER_FILTER_LOADED](store.state);
            expect(store.getters[FOLDER_LIST_IS_LOADING](store.state)).toBeFalsy();
        });
    });
});
