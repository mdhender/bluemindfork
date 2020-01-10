import { getMessageByKey } from "../../../src/MailboxItemsStore/getters/getMessageByKey";

const getters = {
    messages: [
        { name: "Message 1 from container 1" },
        { name: "Message 1 from container 2" },
        { name: "Message 2 from container 1" },
        { name: "Message 2 from container 2" }
    ],
    indexOf: key => {
        return ["key1", "key2", "key3", "key4"].indexOf(key);
    }
};
const state = {};

describe("[MailboxItemsStore][getters] : getMessageByKey ", () => {
    test("return message for a given key", () => {
        const message = getMessageByKey(state, getters)("key2");
        expect(message).toEqual({ name: "Message 1 from container 2" });
    });
    test("return undefined if no message match", () => {
        const message = getMessageByKey(state, getters)("key5");
        expect(message).toBeUndefined();
    });
});
