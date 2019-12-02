import { state } from "../../src/MailboxFoldersStore/state";

describe("[MailItemsStore][state] : initial state", () => {
    test("contains an empty object 'items'", () => {
        expect(state.items).toEqual({});
    });
    test("contains a empty array 'itemKeys'", () => {
        expect(state.itemKeys).toEqual([]);
    });
    test("contains a empty array 'itemByContainers'", () => {
        expect(state.itemsByContainer).toEqual({});
    });
    test("no to contain anything else", () => {
        expect(Object.keys(state)).toEqual(["items", "itemKeys", "itemsByContainer"]);
    });
});
