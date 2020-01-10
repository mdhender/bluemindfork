import { currentMailbox } from "../../src/getters/currentMailbox";
import ItemUri from "@bluemind/item-uri";

const getters = {
    my: { mailboxUid: "my mailbox" },
    mailshares: [{ mailboxUid: "a mailbox" }, { mailboxUid: "another mailbox" }]
};

const state = { currentFolderKey: null };

describe("[Mail-WebappStore][getters] : currentMailbox ", () => {
    test("return my information if current folder is from my mailbox", () => {
        state.currentFolderKey = ItemUri.encode("my folder", "my mailbox");
        const result = currentMailbox(state, getters);
        expect(result).toBe(getters.my);
    });
    test("return my information if current folder is from my mailbox", () => {
        state.currentFolderKey = ItemUri.encode("my folder", "another mailbox");
        const result = currentMailbox(state, getters);
        expect(result).toBe(getters.mailshares[1]);
    });
    test("return undefined if current folder is not in a known mailbox", () => {
        state.currentFolderKey = ItemUri.encode("my folder", "some mailbox");
        const result = currentMailbox(state, getters);
        expect(result).toBeUndefined();
    });
});
