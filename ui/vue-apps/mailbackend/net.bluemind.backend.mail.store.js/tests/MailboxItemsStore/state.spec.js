import { state } from "../../src/MailboxItemsStore/state";

describe("[MailItemsStore][state] : Initial state", () => {
    test("contains  'items'", () => {
        expect(state.items).toEqual({});
    });
    test("contains a empty array 'partKeys'", () => {
        expect(state.itemKeys).toEqual([]);
    });
    test("contains an empty object 'partContents'", () => {
        expect(state.partContents).toEqual({});
    });
    test("contains a empty object 'itemsParts'", () => {
        expect(state.itemsParts).toEqual({});
    });
    test("not to contain anything else", () => {
        expect(Object.keys(state)).toEqual(["items", "partContents", "itemKeys", "itemsParts"]);
    });
});
