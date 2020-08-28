import { areAllMessagesSelected } from "../../getters/areAllMessagesSelected";

const state = {
    selectedMessageKeys: []
};
const rootState = {
    mail: {
        messageList: { messageKeys: [1, 2, 3, 4, 5, 6] }
    }
};

describe("[Mail-WebappStore][getters] : areAllMessagesSelected", () => {
    test("All selected", () => {
        state.selectedMessageKeys = [1, 2, 3, 4, 5, 6];
        expect(areAllMessagesSelected(state, undefined, rootState)).toBe(true);
    });
    test("None selected", () => {
        state.selectedMessageKeys = [];
        expect(areAllMessagesSelected(state, undefined, rootState)).toBe(false);
    });
    test("Partially selected", () => {
        state.selectedMessageKeys = [1, 3, 5];
        expect(areAllMessagesSelected(state, undefined, rootState)).toBe(false);
    });
});
