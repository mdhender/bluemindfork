import { storeItems } from "../../../src/MailboxItemsStore/mutations/storeItems";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");

describe("[MailboxItemsStore][mutations] : storeItems", () => {
    test("transform uid into items uri", () => {
        const state = { itemKeys: [], items: {}, itemsParts: {} };
        const items = [
            { internalId: 1, value: { name: "Item 1" } },
            { internalId: 2, value: { name: "Item 2" } },
            { internalId: 3, value: { name: "Item 3" } }
        ];
        storeItems(state, { items, folderUid: "container:uid" });
        let result = { items: {}, itemKeys: [], itemsParts: {} };
        items.forEach(item => {
            const uri = ItemUri.encode(item.internalId, "container:uid");
            result.itemKeys.push(uri);
            result.items[uri] = item;
            result.itemsParts[uri] = [];
        });
        expect(state).toEqual(result);
    });
    test("add items not already present in state, update the others", () => {
        const state = { itemKeys: [], items: {}, itemsParts: {} };
        const items = [
            { internalId: 1, value: { name: "Item 1" } },
            { internalId: 2, value: { name: "Item 2" } },
            { internalId: 3, value: { name: "Item 3" } }
        ];
        storeItems(state, { items, folderUid: "container:uid" });
        const update = [
            { internalId: 2, value: { name: "Item 2 Updated" } },
            { internalId: 4, value: { name: "Item 4" } }
        ];
        storeItems(state, { items: update, folderUid: "container:uid" });
        let result = { items: {}, itemKeys: [], itemsParts: {} };
        items.concat(update).forEach(item => {
            const uri = ItemUri.encode(item.internalId, "container:uid");
            result.items[uri] = item;
            if (!result.itemKeys.includes(uri)) {
                result.itemKeys.push(uri);
                result.itemsParts[uri] = [];
            }
        });
        expect(state).toEqual(result);
    });
    test("support items from different containers", () => {
        const result = { items: {} },
            state = { itemKeys: [], items: {}, itemsParts: {} };

        const itemsFromContainer1 = [{ internalId: 1, value: { name: "Item 1" } }];
        storeItems(state, { items: itemsFromContainer1, folderUid: "container:1:uid" });
        itemsFromContainer1.forEach(item => {
            const uri = ItemUri.encode(item.internalId, "container:1:uid");
            result.items[uri] = item;
        });

        const itemsFromContainer2 = [
            { internalId: 1, value: { name: "Item 1" } },
            { internalId: 2, value: { name: "Item 2" } }
        ];
        storeItems(state, { items: itemsFromContainer2, folderUid: "container:2:uid" });
        itemsFromContainer2.forEach(item => {
            const uri = ItemUri.encode(item.internalId, "container:2:uid");
            result.items[uri] = item;
        });
        expect(state.itemKeys.length).toBe(3); // or not to be
        expect(state.itemKeys).toEqual(Object.keys(result.items));
        expect(state.items).toEqual(result.items);
    });
    test("update only the needed part of the state", () => {
        const state = { itemKeys: [], items: {}, itemsParts: {} };
        const items = [{ internalId: 1, value: { name: "Item 1" } }];
        storeItems(state, { items, folderUid: "dummy" });
        expect(Object.keys(state)).toEqual(["itemKeys", "items", "itemsParts"]);
    });
});
