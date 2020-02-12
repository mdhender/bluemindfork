import { messages } from "../../../src/MailboxItemsStore/getters/messages";
import Message from "../../../src/MailboxItemsStore/Message";
const items = {
    key1: {
        internalId: "a",
        value: { name: "A", flags: [], data: null, body: { recipients: [] } }
    },
    key2: {
        internalId: "b",
        value: { name: "B", flags: [], data: null, body: { recipients: [] } }
    },
    key3: {
        internalId: "c",
        value: { name: "C", flags: [], data: null, body: { recipients: [] } }
    },
    key4: {
        internalId: "a.d",
        value: { name: "D", flags: [], data: "a", body: { recipients: [] } }
    },
    key5: {
        internalId: "a.e",
        value: { name: "E", flags: [], data: "a", body: { recipients: [] } }
    },
    key6: {
        internalId: "b.f",
        value: { name: "F", flags: [], data: "b", body: { recipients: [] } }
    },
    key7: {
        internalId: "b.g",
        value: { name: "G", flags: [], data: "b", body: { recipients: [] } }
    },
    key8: {
        internalId: "b.h",
        value: { name: "H", flags: [], data: "b", body: { recipients: [] } }
    },
    key9: {
        internalId: "b.h.i",
        value: { name: "I", flags: [], data: "b.h", body: { recipients: [] } }
    },
    key10: {
        internalId: "b.h.j",
        value: { name: "J", flags: [], data: "b.h", body: { recipients: [] } }
    },
    key11: {
        internalId: "b.h.j.k",
        value: { name: "K", flags: [], data: "b.h.j", body: { recipients: [] } }
    },
    key12: {
        internalId: "a.e.l",
        value: { name: "L", flags: [], data: "a.e", body: { recipients: [] } }
    }
};
const itemKeys = ["key1", "key4", "key5", "key12", "key2", "key6", "key7", "key8", "key9", "key10", "key11"];
const state = {
    items,
    itemKeys
};

describe("[MailboxItemsStore][getters] : messages ", () => {
    test("contains all items with a key in itemKeys ", () => {
        const result = messages(state);
        expect(result.length).toEqual(itemKeys.length);
    });
    test("return Message instances ", () => {
        const result = messages(state);
        result.forEach(f => expect(f).toBeInstanceOf(Message));
    });
    test("return value match item key order", () => {
        const result = messages(state);
        itemKeys.forEach((k, i) => expect(k).toEqual(result[i].key));
    });
    test("does not contains items not in itemKeys", () => {
        const result = messages(state);
        expect(result).toEqual(expect.not.arrayContaining([items.key3]));
    });
});
