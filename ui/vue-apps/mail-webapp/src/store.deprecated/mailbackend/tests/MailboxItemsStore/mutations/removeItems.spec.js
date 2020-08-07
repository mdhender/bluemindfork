import { removeItems } from "../../../MailboxItemsStore/mutations/removeItems";

const state = {};
describe("[MailItemsStore][mutations] : RemoveItems", () => {
    beforeEach(() => {
        Object.assign(state, {
            itemKeys: ["key1", "key2", "key3"],
            items: {
                key1: { value: "" },
                key2: { value: "" },
                key3: { value: "" }
            },
            partContents: {
                "key1/1": "Part content",
                "key1/2": "Part content",
                "key2/1": "Part content",
                "key3/1": "Part content"
            },
            itemsParts: {
                key1: ["key1/1", "key1/2"],
                key2: ["key2/1"],
                key3: ["key3/1"]
            }
        });
    });
    test("remove all data related to the given uid from state", () => {
        const keys = ["key1", "key2"];
        removeItems(state, keys);
        expect(state.itemKeys).toEqual(state.itemKeys.filter(k => !keys.includes(k)));
        expect(Object.keys(state.items)).toEqual(state.itemKeys.filter(k => !keys.includes(k)));
        expect(state.partContents).toEqual({ "key3/1": "Part content" });
        expect(Object.keys(state.itemsParts)).toEqual(state.itemKeys.filter(k => !keys.includes(k)));
    });
    test("mutate only needed key of state", () => {
        const keys = ["key2"];
        removeItems(state, keys);
        expect(Object.keys(state)).toEqual(["itemKeys", "items", "partContents", "itemsParts"]);
    });
});
