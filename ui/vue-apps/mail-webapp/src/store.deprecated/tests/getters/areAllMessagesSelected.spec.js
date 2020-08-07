import { areAllMessagesSelected } from "../../getters/areAllMessagesSelected";

const state = {
    selectedMessageKeys: [],
    messages: {
        itemKeys: [1, 2, 3, 4, 5, 6]
    }
};

describe("[Mail-WebappStore][getters] : areAllMessagesSelected", () => {
    test("All selected", () => {
        state.selectedMessageKeys = [1, 2, 3, 4, 5, 6];
        expect(areAllMessagesSelected(state)).toBe(true);
    });
    test("None selected", () => {
        state.selectedMessageKeys = [];
        expect(areAllMessagesSelected(state)).toBe(false);
    });
    test("Partially selected", () => {
        state.selectedMessageKeys = [1, 3, 5];
        expect(areAllMessagesSelected(state)).toBe(false);
    });
});
