import { deleteAllSelectedMessages } from "../../src/mutations/deleteAllSelectedMessages";

const state = {
    selectedMessageKeys: [2, 3, 4],
    messages: {
        itemKeys: [1, 2, 3, 4, 5, 6]
    }
};

describe("[Mail-WebappStore][mutations] : deleteAllSelectedMessages", () => {
    test("Basic", () => {
        deleteAllSelectedMessages(state);
        expect(state.selectedMessageKeys).toEqual([]);
    });
});
