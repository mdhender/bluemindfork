import { messages } from "../../../MailboxItemsStore/getters/messages";
import Message from "../../../MailboxItemsStore/Message";

const items = {
    key1: {
        remote: {
            internalId: "a",
            value: { name: "A", flags: [], data: null, body: { recipients: [], headers: [] } }
        }
    },
    key2: {
        remote: {
            internalId: "b",
            value: { name: "B", flags: [], data: null, body: { recipients: [], headers: [] } }
        }
    },
    key3: {
        remote: {
            internalId: "c",
            value: { name: "C", flags: [], data: null, body: { recipients: [], headers: [] } }
        }
    },
    key4: {
        remote: {
            internalId: "a.d",
            value: { name: "D", flags: [], data: "a", body: { recipients: [], headers: [] } }
        }
    },
    key5: {
        remote: {
            internalId: "a.e",
            value: { name: "E", flags: [], data: "a", body: { recipients: [], headers: [] } }
        }
    },
    key6: {
        remote: {
            internalId: "b.f",
            value: { name: "F", flags: [], data: "b", body: { recipients: [], headers: [] } }
        }
    },
    key7: {
        remote: {
            internalId: "b.g",
            value: { name: "G", flags: [], data: "b", body: { recipients: [], headers: [] } }
        }
    },
    key8: {
        remote: {
            internalId: "b.h",
            value: { name: "H", flags: [], data: "b", body: { recipients: [], headers: [] } }
        }
    },
    key9: {
        remote: {
            internalId: "b.h.i",
            value: { name: "I", flags: [], data: "b.h", body: { recipients: [], headers: [] } }
        }
    },
    key10: {
        remote: {
            internalId: "b.h.j",
            value: { name: "J", flags: [], data: "b.h", body: { recipients: [], headers: [] } }
        }
    },
    key11: {
        remote: {
            internalId: "b.h.j.k",
            value: { name: "K", flags: [], data: "b.h.j", body: { recipients: [], headers: [] } }
        }
    },
    key12: {
        remote: {
            internalId: "a.e.l",
            value: { name: "L", flags: [], data: "a.e", body: { recipients: [], headers: [] } }
        }
    }
};
const itemKeys = ["key1", "key4", "key5", "key12", "key2", "key6", "key7", "key8", "key9", "key10", "key11"];
const rootState = {
    mail: {
        messages: items,
        messageList: {
            messageKeys: itemKeys
        }
    }
};
const rootGetter = {
    "mail/isLoaded": key => items[key]
};

describe("[MailboxItemsStore][getters] : messages ", () => {
    test("contains all items with a key in itemKeys ", () => {
        const result = messages(undefined, undefined, rootState, rootGetter);
        expect(result.length).toEqual(itemKeys.length);
    });
    test("return Message instances ", () => {
        const result = messages(undefined, undefined, rootState, rootGetter);
        result.forEach(f => expect(f).toBeInstanceOf(Message));
    });
    test("return value match item key order", () => {
        const result = messages(undefined, undefined, rootState, rootGetter);
        itemKeys.forEach((k, i) => expect(k).toEqual(result[i].key));
    });
    test("does not contains items not in itemKeys", () => {
        const result = messages(undefined, undefined, rootState, rootGetter);
        expect(result).toEqual(expect.not.arrayContaining([items.key3]));
    });
});
