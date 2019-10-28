import { removeItems } from "../../../src/MailboxItemsStore/mutations/removeItems";

describe("MailItems store: Remove items mutation", () => {
    
    const state = {
        sortedIds: [ 17289 ],
        items: {
            "17289": { "value": "okok" }
        },
        parts: {
            "17289": { "1": "This is a very simple mail content." }
        }
    };

    test("remove one item clear the state", () => {
        const ids = [ 17289 ];
        removeItems(state, ids);
        expect(state.sortedIds).toEqual([]);
        expect(state.items).toEqual({});
        expect(state.parts).toEqual({});
    });
});