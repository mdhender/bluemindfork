import { addAllToSelectedMessages } from "../../src/mutations/addAllToSelectedMessages";

const state = {
    selectedMessageKeys: []
};

describe("[Mail-WebappStore][mutations] : addAllToSelectedMessages", () => {
    test("Basic", () => {
        const itemKeys = [1, 2, 3, 4, 5, 6];
        addAllToSelectedMessages(state, itemKeys);
        expect(state.selectedMessageKeys).toEqual([1, 2, 3, 4, 5, 6]);
    });
});
