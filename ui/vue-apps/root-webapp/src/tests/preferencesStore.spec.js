import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import preferencesStore from "../preferences/store";

describe("Preferences store", () => {
    let store;
    Vue.use(Vuex);

    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(preferencesStore));
    });

    test("SET_OFFSET mutation", () => {
        preferencesStore.mutations.SET_OFFSET(store.state, 42);
        expect(store.state.offset).toEqual(42);
    });

    test("SET_SEARCH mutation", () => {
        preferencesStore.mutations.SET_SEARCH(store.state, "my search");
        expect(store.state.search).toEqual("my search");
    });

    test("TOGGLE_PREFERENCES mutation", () => {
        expect(store.state.showPreferences).toEqual(false);
        preferencesStore.mutations.TOGGLE_PREFERENCES(store.state);
        expect(store.state.showPreferences).toEqual(true);
        preferencesStore.mutations.TOGGLE_PREFERENCES(store.state);
        expect(store.state.showPreferences).toEqual(false);
    });

    test("SET_SECTIONS mutation", () => {
        preferencesStore.mutations.SET_SECTIONS(store.state, [{ id: "main" }, { id: "mail" }]);
        expect(store.state.sectionById).toEqual({ main: { id: "main" }, mail: { id: "mail" } });
    });

    test("SET_SELECTED_SECTION mutation", () => {
        preferencesStore.mutations.SET_SELECTED_SECTION(store.state, "main");
        expect(store.state.selectedSectionId).toEqual("main");
    });

    test("SECTIONS getter", () => {
        store.state.sectionById = { main: { id: "main", visible: true }, mail: { id: "mail", visible: false } };
        expect(preferencesStore.getters.SECTIONS(store.state)).toEqual([{ id: "main", visible: true }]);
    });

    test("GET_GROUP getter", () => {
        store.state.sectionById = {
            main: { id: "main", visible: true, categories: [{ groups: [{ id: "find-me", check: "ok" }] }] }
        };
        expect(preferencesStore.getters.GET_GROUP(store.state, store.getters)("find-me")).toEqual({
            id: "find-me",
            check: "ok"
        });
        expect(preferencesStore.getters.GET_GROUP(store.state, store.getters)("dont-exist")).toBeFalsy();
    });

    test("GET_SECTION getter", () => {
        store.state.sectionById = {
            main: { id: "main", check: "ok", visible: true, categories: [{ groups: [{ id: "find-me" }] }] }
        };
        expect(preferencesStore.getters.GET_SECTION(store.state, store.getters)("find-me")).toMatchObject({
            check: "ok"
        });
        expect(preferencesStore.getters.GET_SECTION(store.state, store.getters)("dont-exist")).toBeFalsy();
    });

    test("HAS_SEARCH getters", () => {
        store.state.search = " Blabla ";
        expect(preferencesStore.getters.HAS_SEARCH(store.state, store.getters)).toEqual(true);

        store.state.search = " ";
        expect(preferencesStore.getters.HAS_SEARCH(store.state, store.getters)).toEqual(false);

        store.state.search = "";
        expect(preferencesStore.getters.HAS_SEARCH(store.state, store.getters)).toEqual(false);
    });

    test("SEARCH_PATTERN getters", () => {
        store.state.search = " Blabla ";
        expect(preferencesStore.getters.SEARCH_PATTERN(store.state)).toEqual("blabla");
    });

    test("SET_EXTERNAL_ACCOUNTS mutation", () => {
        preferencesStore.mutations.SET_EXTERNAL_ACCOUNTS(store.state, ["a", "b", "c"]);
        expect(store.state.externalAccounts).toEqual(["a", "b", "c"]);
    });
});
