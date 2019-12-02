import { count } from "../../../src/MailboxItemsStore/getters/count";

describe("[MailboxItemsStore][getters] : count ", () => {
    test("return number of items in list ", () => {
        const state = { items: { 1: {}, 2: {} }, itemKeys: [1, 2] };
        expect(count(state)).toEqual(2);
        state.itemKeys = [];
        state.items = {};
        expect(count(state)).toEqual(0);
    });
    test("return the number of listed item even if state.item and state.itemKeys are different", () => {
        const state = { items: {}, itemKeys: [1, 2] };
        expect(count(state)).toEqual(2);
    });
    test("return the size of the itemKeys not the number of item within", () => {
        const state = { items: {}, itemKeys: new Array(100) };
        expect(count(state)).toEqual(100);
    });
});
