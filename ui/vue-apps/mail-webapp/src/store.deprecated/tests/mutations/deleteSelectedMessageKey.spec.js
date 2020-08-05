import { deleteSelectedMessageKey } from "../../src/mutations/deleteSelectedMessageKey";

const state = {
    selectedMessageKeys: [2, 3, 4],
    messages: {
        itemKeys: [1, 2, 3, 4, 5, 6]
    }
};

describe("[Mail-WebappStore][mutations] : deleteSelectedMessageKey", () => {
    test("Basic", () => {
        deleteSelectedMessageKey(state, 3);
        expect(state.selectedMessageKeys).toEqual([2, 4]);
    });
});
