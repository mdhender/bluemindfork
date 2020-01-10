import { folders } from "../../../src/MailboxFoldersStore/getters/folders";
import Folder from "../../../src/MailboxFoldersStore/Folder";

const items = {
    key1: { uid: "a", value: { name: "A", parentUid: null } },
    key2: { uid: "b", value: { name: "B", parentUid: null } },
    key3: { uid: "c", value: { name: "C", parentUid: null } },
    key4: { uid: "a.d", value: { name: "D", parentUid: "a" } },
    key5: { uid: "a.e", value: { name: "E", parentUid: "a" } },
    key6: { uid: "b.f", value: { name: "F", parentUid: "b" } },
    key7: { uid: "b.g", value: { name: "G", parentUid: "b" } },
    key8: { uid: "b.h", value: { name: "H", parentUid: "b" } },
    key9: { uid: "b.h.i", value: { name: "I", parentUid: "b.h" } },
    key10: { uid: "b.h.j", value: { name: "J", parentUid: "b.h" } },
    key11: { uid: "b.h.j.k", value: { name: "K", parentUid: "b.h.j" } },
    key12: { uid: "a.e.l", value: { name: "L", parentUid: "a.e" } }
};
const itemKeys = ["key1", "key4", "key5", "key12", "key2", "key6", "key7", "key8", "key9", "key10", "key11"];
const state = {
    items,
    itemKeys
};

describe("[MailFoldersStore][getters] : folders ", () => {
    test("return Folder instances ", () => {
        const result = folders(state);
        result.forEach(f => expect(f).toBeInstanceOf(Folder));
    });
    test("return value match item key order", () => {
        const result = folders(state);
        result.forEach((f, i) => expect(f.key).toEqual(itemKeys[i]));
    });
    test("does not contains items not in itemKeys", () => {
        const result = folders(state);
        expect(result).toEqual(expect.not.arrayContaining([items.key3]));
    });
});
