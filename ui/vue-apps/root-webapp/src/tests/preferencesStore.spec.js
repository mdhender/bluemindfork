import preferencesStore from "../preferencesStore";

describe("Preferences store", () => {
    let context;

    beforeEach(() => {
        context = {
            state: { offset: 0, showPreferences: false, selectedSectionCode: "", sectionByCode: {} },
            commit: jest.fn()
        };
    });

    test("SET_OFFSET mutation", () => {
        preferencesStore.mutations.SET_OFFSET(context.state, 42);
        expect(context.state.offset).toEqual(42);
    });

    test("TOGGLE_PREFERENCES mutation", () => {
        expect(context.state.showPreferences).toEqual(false);
        preferencesStore.mutations.TOGGLE_PREFERENCES(context.state);
        expect(context.state.showPreferences).toEqual(true);
        preferencesStore.mutations.TOGGLE_PREFERENCES(context.state);
        expect(context.state.showPreferences).toEqual(false);
    });

    test("SET_SECTIONS mutation", () => {
        preferencesStore.mutations.SET_SECTIONS(context.state, [{ code: "main" }, { code: "mail" }]);
        expect(context.state.sectionByCode).toEqual({ main: { code: "main" }, mail: { code: "mail" } });
    });

    test("SET_SELECTED_SECTION mutation", () => {
        preferencesStore.mutations.SET_SELECTED_SECTION(context.state, "main");
        expect(context.state.selectedSectionCode).toEqual("main");
    });

    test("SECTIONS getter", () => {
        context.state.sectionByCode = { main: { code: "main" }, mail: { code: "mail" } };
        expect(preferencesStore.getters.SECTIONS(context.state)).toEqual([{ code: "main" }, { code: "mail" }]);
    });
});
