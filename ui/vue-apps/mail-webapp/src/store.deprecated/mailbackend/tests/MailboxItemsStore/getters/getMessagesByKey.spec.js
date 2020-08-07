import { getMessagesByKey } from "../../../MailboxItemsStore/getters/getMessagesByKey";

const getters = {
    messages: [
        { name: "Message 1 from container 1", key: "key1" },
        { name: "Message 1 from container 2", key: "key2" },
        { name: "Message 2 from container 1", key: "key3" },
        { name: "Message 2 from container 2", key: "key4" }
    ],
    indexOf: key => {
        return ["key1", "key2", "key3", "key4"].indexOf(key);
    }
};
const state = {};

describe("[MailboxItemsStore][getters] : getMessagesByKey ", () => {
    test("return message for a given key", () => {
        const message = getMessagesByKey(state, getters)(["key2"])[0];
        expect(message).toEqual({ name: "Message 1 from container 2", key: "key2" });
    });
    test("return undefined if no message match", () => {
        const message = getMessagesByKey(state, getters)(["key5"])[0];
        expect(message).toBeUndefined();
    });
    test("return messages for given keys", () => {
        const messages = getMessagesByKey(state, getters)(["key2", "key4", "keyKate"]);
        expect(messages).toEqual([
            { name: "Message 1 from container 2", key: "key2" },
            { name: "Message 2 from container 2", key: "key4" }
        ]);
    });
});
