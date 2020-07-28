import { isMessageSelected } from "../../src/getters/isMessageSelected";

const state = {
    selectedMessageKeys: [],
    messages: {
        itemKeys: [2, 4, 6]
    }
};

describe("[Mail-WebappStore][getters] : isMessageSelected", () => {
    test("Yes", () => {
        state.selectedMessageKeys = [4];
        expect(isMessageSelected(state)(4)).toBe(true);
    });
    test("No", () => {
        state.selectedMessageKeys = [5];
        expect(isMessageSelected(state)(4)).toBe(false);
    });
});
