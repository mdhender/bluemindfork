import { setItemKeys } from "../../../src/MailboxItemsStore/mutations/setItemKeys";
import ItemUri from "@bluemind/item-uri";

describe("[MailItemsStore][mutations] : setItemsKeys", () => {
    test("transform message ids into items uri", () => {
        const state = { itemKeys: undefined };
        const ids = [1, 2, 3, 4, 5, 6, 7];
        setItemKeys(state, { ids, folderUid: "container:uid" });
        expect(state.itemKeys).toEqual(ids.map(id => ItemUri.encode(id, "container:uid")));
    });
    test("replace the precedent list", () => {
        const state = { itemKeys: [1, 2, 3] };
        const ids = [999, 1000];
        setItemKeys(state, { ids, folderUid: "container:uid" });
        expect(state.itemKeys).toEqual(
            expect.not.arrayContaining([1, 2, 3].map(id => ItemUri.encode(id, "container:uid")))
        );
    });
    test("mutate only needed key of state", () => {
        const state = { itemKeys: undefined };
        const ids = [1, 2, 3, 4, 5, 6, 7];
        setItemKeys(state, { ids, folderUid: "container:uid" });
        expect(Object.keys(state)).toEqual(["itemKeys"]);
    });
});
