import { storeItems } from "../../../src/MailboxFoldersStore/mutations/storeItems";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");

describe("[MailFoldersStore][mutations] : storeItems", () => {
    test("transform uid into items uri", () => {
        const state = { itemKeys: [], items: {}, itemsByContainer: {} };
        const items = [
            { uid: "item-1", value: { name: "Item 1" } },
            { uid: "item-2", value: { name: "Item 2" } },
            { uid: "item-3", value: { name: "Item 3" } }
        ];
        const mailboxUid = "container:uid";
        storeItems(state, { items, mailboxUid });
        let result = { items: {}, itemKeys: [], itemsByContainer: { [mailboxUid]: [] } };
        items.forEach(item => {
            const uri = ItemUri.encode(item.uid, mailboxUid);
            result.itemKeys.push(uri);
            result.items[uri] = item;
            result.itemsByContainer[mailboxUid].push(uri);
        });
        expect(state).toEqual(result);
    });
    test("add items not already present in state, update the others", () => {
        const state = { itemKeys: [], items: {}, itemsByContainer: {} };
        const items = [
            { uid: "item-1", value: { name: "Item 1" } },
            { uid: "item-2", value: { name: "Item 2" } },
            { uid: "item-3", value: { name: "Item 3" } }
        ];
        const mailboxUid = "container:uid";
        storeItems(state, { items, mailboxUid });
        const update = [
            { uid: "item-2", value: { name: "Item 2 Updated" } },
            { uid: "item-4", value: { name: "Item 4" } }
        ];
        storeItems(state, { items: update, mailboxUid });
        let result = { items: {}, itemKeys: [], itemsByContainer: { [mailboxUid]: [] } };
        items.concat(update).forEach(item => {
            const uri = ItemUri.encode(item.uid, mailboxUid);
            result.items[uri] = item;
            if (!result.itemKeys.includes(uri)) {
                result.itemKeys.push(uri);
                result.itemsByContainer[mailboxUid].push(uri);
            }
        });
        expect(state).toEqual(result);
    });
    test("support items from different containers", () => {
        const result = { items: {}, itemsByContainer: { "container:1:uid": [], "container:2:uid": [] } },
            state = { itemKeys: [], items: {}, itemsByContainer: {} };

        const itemsFromContainer1 = [{ uid: "item-1", value: { name: "Item 1" } }];
        storeItems(state, { items: itemsFromContainer1, mailboxUid: "container:1:uid" });
        itemsFromContainer1.forEach(item => {
            const uri = ItemUri.encode(item.uid, "container:1:uid");
            result.items[uri] = item;
            result.itemsByContainer["container:1:uid"].push(uri);
        });

        const itemsFromContainer2 = [
            { uid: "item-1", value: { name: "Item 1" } },
            { uid: "item-2", value: { name: "Item 2" } }
        ];
        storeItems(state, { items: itemsFromContainer2, mailboxUid: "container:2:uid" });
        itemsFromContainer2.forEach(item => {
            const uri = ItemUri.encode(item.uid, "container:2:uid");
            result.items[uri] = item;
            result.itemsByContainer["container:2:uid"].push(uri);
        });
        expect(state.itemKeys.length).toBe(3); // or not to be
        expect(state.itemKeys).toEqual(Object.keys(result.items));
        expect(state.items).toEqual(result.items);
        expect(state.itemsByContainer).toEqual(result.itemsByContainer);
    });
    test("update only the needed part of the state", () => {
        const state = { itemKeys: [], items: {}, itemsByContainer: {} };
        const items = [{ uid: "item-1", value: { name: "Item 1" } }];
        storeItems(state, { items, mailboxUid: "dummy" });
        expect(Object.keys(state)).toEqual(["itemKeys", "items", "itemsByContainer"]);
    });
});
