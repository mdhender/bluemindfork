import { addSelectedMessageKey } from "../../src/mutations/addSelectedMessageKey";

const state = {
    selectedMessageKeys: [2, 4],
    messages: {
        itemKeys: [1, 2, 3, 4, 5, 6]
    }
};

describe("[Mail-WebappStore][mutations] : addSelectedMessageKey", () => {
    test("Basic", () => {
        addSelectedMessageKey(state, 3);
        expect(state.selectedMessageKeys).toEqual([2, 4, 3]);
    });
});
