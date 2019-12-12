import { setMessageFilter } from "../../src/mutations/setMessageFilter";

describe("[Mail-WebappStore][mutations] : setMessageFilter", () => {
    const state = {};
    beforeEach(() => {
        state.messageFilter = "filter1";
    });
    test("Mutate currentFolderKey", () => {
        setMessageFilter(state, "filter2");
        expect(state.messageFilter).toEqual("filter2");
    });
    test("Only mutate messageFilter", () => {
        setMessageFilter(state, "filter2");
        expect(Object.keys(state)).toEqual(["messageFilter"]);
    });
});
