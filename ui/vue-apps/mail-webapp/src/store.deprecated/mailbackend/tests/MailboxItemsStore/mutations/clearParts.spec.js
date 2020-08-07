import { clearParts } from "../../../MailboxItemsStore/mutations/clearParts";

jest.mock("@bluemind/inject");

describe("[MailItemsStore][mutations] : clearParts", () => {
    test("remove all items from state", () => {
        const state = {
            partContents: {
                "key1/1": { content: "text" },
                "key2/2": { content: "images" }
            },
            itemsParts: {
                key1: ["key1/1"],
                key2: ["key2/1"]
            }
        };
        clearParts(state);
        expect(state).toEqual({ partContents: {}, itemsParts: { key1: [], key2: [] } });
    });
    test("mutate only parts", () => {
        const state = {
            partContents: {},
            itemsParts: {}
        };
        clearParts(state);
        expect(Object.keys(state)).toEqual(["partContents", "itemsParts"]);
    });
});
