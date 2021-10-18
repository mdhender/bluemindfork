import preferencesStore from "../preferencesStore";

describe("Preferences store", () => {
    let context;

    beforeEach(() => {
        context = {
            state: { offset: 0, showPreferences: false, selectedSectionId: "", sectionById: {} },
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
        preferencesStore.mutations.SET_SECTIONS(context.state, [{ id: "main" }, { id: "mail" }]);
        expect(context.state.sectionById).toEqual({ main: { id: "main" }, mail: { id: "mail" } });
    });

    test("SET_SELECTED_SECTION mutation", () => {
        preferencesStore.mutations.SET_SELECTED_SECTION(context.state, "main");
        expect(context.state.selectedSectionId).toEqual("main");
    });

    test("SECTIONS getter", () => {
        context.state.sectionById = { main: { id: "main", visible: true }, mail: { id: "mail", visible: false } };
        expect(preferencesStore.getters.SECTIONS(context.state)).toEqual([{ id: "main", visible: true }]);
    });
});
