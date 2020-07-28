import { removeItems } from "../../../src/MailboxFoldersStore/mutations/removeItems";
import ItemUri from "@bluemind/item-uri";

jest.mock("@bluemind/inject");

const mailboxUid = "container:uid";
const key1 = ItemUri.encode("item-1", mailboxUid);
const key2 = ItemUri.encode("item-2", mailboxUid);
const key3 = ItemUri.encode("item-3", mailboxUid);
const items = {
    [key1]: { uid: "item-1", value: { name: "Item 1" }, key: key1 },
    [key2]: { uid: "item-2", value: { name: "Item 2" }, key: key2 },
    [key3]: { uid: "item-3", value: { name: "Item 3" }, key: key3 }
};
const itemKeys = [key1, key2, key3];
const state = { itemKeys, items, itemsByContainer: { [mailboxUid]: [key1, key2, key3] } };

describe("[MailFoldersStore][mutations] : removeItems", () => {
    test("Basic", () => {
        removeItems(state, [key1, key3]);
        let result = {
            items: { [key2]: { uid: "item-2", value: { name: "Item 2" }, key: key2 } },
            itemKeys: [key2],
            itemsByContainer: { [mailboxUid]: [key2] }
        };
        expect(state).toEqual(result);
    });
});
