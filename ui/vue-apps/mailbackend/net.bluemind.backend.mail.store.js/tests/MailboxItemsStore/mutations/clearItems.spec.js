import { clearItems } from "../../../src/MailboxItemsStore/mutations/clearItems";

jest.mock("@bluemind/inject");

describe("[MailItemsStore][mutations] : clearItems", () => {
    test("remove all items from state", () => {
        const state = {
            items: {
                key1: { uid: "item-1", value: { name: "Item 1" } },
                key2: { uid: "item-2", value: { name: "Item 2" } },
                key3: { uid: "item-3", value: { name: "Item 3" } }
            },
            itemKeys: ["key1", "key2", "key3"]
        };
        clearItems(state);
        expect(state).toEqual({ items: {}, itemKeys: [] });
    });
    test("mutate only items", () => {
        const state = {
            items: {}
        };
        clearItems(state);
        expect(Object.keys(state)).toEqual(["items", "itemKeys"]);
    });
});
