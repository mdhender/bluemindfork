import { storePartContent } from "../../../MailboxItemsStore/mutations/storePartContent";
import { PartKey } from "../../../MailboxItemsStore/PartKey";

describe("[MailItemsStore][mutations] : storePartContent", () => {
    test("store part content and part/item mapping", () => {
        const messageKey = "key1";
        const state = { partContents: {}, itemsParts: { [messageKey]: [] } };

        storePartContent(state, { messageKey, address: "1.1", content: "content..." });

        const key = PartKey.encode("1.1", messageKey);
        expect(state.partContents).toEqual({ [key]: "content..." });
        expect(state.itemsParts).toEqual({ [messageKey]: [key] });
    });
    test("add part contents to state if the part is not already present", () => {
        const messageKey = "key1";
        const state = { partContents: {}, itemsParts: { [messageKey]: [] } };
        storePartContent(state, { messageKey, address: "1.1", content: "content..." });
        storePartContent(state, { messageKey, address: "1.2", content: "content2..." });

        const key = PartKey.encode("1.1", messageKey);
        const key2 = PartKey.encode("1.2", messageKey);
        expect(state.partContents).toEqual({ [key]: "content...", [key2]: "content2..." });
        expect(state.itemsParts).toEqual({ [messageKey]: [key, key2] });
    });

    test("update part contents to state if the part is  already present", () => {
        const messageKey = "key1";
        const state = { partContents: {}, itemsParts: { [messageKey]: [] } };
        storePartContent(state, { messageKey, address: "1.1", content: "content..." });
        storePartContent(state, { messageKey, address: "1.1", content: "content..." });

        const key = PartKey.encode("1.1", messageKey);
        expect(state.partContents).toEqual({ [key]: "content..." });
        expect(state.itemsParts).toEqual({ [messageKey]: [key] });
    });

    test("support having part from different messages", () => {
        const message1Key = "key1";
        const message2Key = "key2";
        const state = { partContents: {}, itemsParts: { [message1Key]: [], [message2Key]: [] } };
        storePartContent(state, { messageKey: message1Key, address: "1.1", content: "content1..." });
        storePartContent(state, { messageKey: message2Key, address: "1.1", content: "content2..." });

        const key1 = PartKey.encode("1.1", message1Key);
        const key2 = PartKey.encode("1.1", message2Key);
        expect(state.partContents).toEqual({ [key1]: "content1...", [key2]: "content2..." });
        expect(state.itemsParts).toEqual({ [message1Key]: [key1], [message2Key]: [key2] });
    });
    test("mutate only needed key of state", () => {
        const messageKey = "key1";
        const state = { partContents: {}, itemsParts: { [messageKey]: [] } };
        storePartContent(state, { messageKey, address: "1.1", content: "content..." });
        expect(Object.keys(state)).toEqual(["partContents", "itemsParts"]);
    });
});
