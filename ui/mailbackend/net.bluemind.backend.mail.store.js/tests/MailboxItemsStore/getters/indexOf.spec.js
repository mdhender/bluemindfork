import { indexOf } from "../../../src/MailboxItemsStore/getters/indexOf";

const itemKeys = ["key1", "key4", "key5", "key12", "key2", "key6", "key7", "key8", "key9", "key10", "key11"];
const state = {
    itemKeys
};

describe("[MailboxItemsStore][getters] : indexOf ", () => {
    test("return index of a message for a given key", () => {
        expect(indexOf(state)("key4")).toEqual(1);
    });
    test("return -1 if no key match", () => {
        expect(indexOf(state)("unknown")).toEqual(-1);
    });
});
