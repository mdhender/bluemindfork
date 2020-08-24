import getters from "../getters";
import { DEFAULT_FOLDER_NAMES } from "../helpers/DefaultFolders";

describe("getters", () => {
    test("FOLDER_BY_PATH", () => {
        const folder = {
            name: "foo",
            path: "foo",
            key: "123"
        };
        const state = {
            "123": folder
        };
        expect(getters.FOLDER_BY_PATH(state)("foo")).toEqual(folder);
        expect(getters.FOLDER_BY_PATH(state)("whatever")).toEqual(undefined);
    });

    test("FOLDER_BY_MAILBOX", () => {
        const state = {
            "1": { key: "1", mailbox: "mine" },
            "2": { key: "2", mailbox: "mine" },
            "3": { key: "3", mailbox: "other" }
        };
        expect(getters.FOLDER_BY_MAILBOX(state)("mine")).toEqual([
            { key: "1", mailbox: "mine" },
            { key: "2", mailbox: "mine" }
        ]);
        expect(getters.FOLDER_BY_MAILBOX(state)("other")).toEqual([{ key: "3", mailbox: "other" }]);
    });

    test("HAS_CHILDREN_GETTER", () => {
        const state = {
            "1": { key: "1", parent: null },
            "2": { key: "2", parent: "1" },
            "3": { key: "3", parent: "unknown" }
        };
        expect(getters.HAS_CHILDREN_GETTER(state)("1")).toEqual(true);
        expect(getters.HAS_CHILDREN_GETTER(state)("2")).toEqual(false);
        expect(getters.HAS_CHILDREN_GETTER(state)("3")).toEqual(false);
    });

    test("MAILSHARE_FOLDERS", async () => {
        const state = {
            "1": { key: "1", mailbox: "A" },
            "2": { key: "2", mailbox: "unknown" },
            "3": { key: "3", mailbox: "B" },
            "4": { key: "4", mailbox: "C" }
        };
        const mockedGetters = { MAILSHARE_KEYS: ["A", "C"] };
        expect(getters.MAILSHARE_FOLDERS(state, mockedGetters)).toEqual(["1", "4"]);
    });

    test("MY_MAILBOX_FOLDERS", async () => {
        const state = {
            "1": { key: "1", mailbox: "A" },
            "2": { key: "2", mailbox: "unknown" },
            "3": { key: "3", mailbox: "B" },
            "4": { key: "4", mailbox: "C" }
        };
        const mockedGetters = { MY_MAILBOX_KEY: "B" };
        expect(getters.MY_MAILBOX_FOLDERS(state, mockedGetters)).toEqual(["3"]);
    });

    test("DEFAULT FOLDERS", async () => {
        const state = {
            "1": { key: "1", imapName: "whatever", mailbox: "myMailbox" },
            "1bis": { key: "1bis", imapName: DEFAULT_FOLDER_NAMES.INBOX, mailbox: "other" },
            "2": { key: "2", imapName: DEFAULT_FOLDER_NAMES.INBOX, mailbox: "myMailbox" },
            "3": { key: "3", imapName: DEFAULT_FOLDER_NAMES.OUTBOX, mailbox: "myMailbox" },
            "4": { key: "4", imapName: DEFAULT_FOLDER_NAMES.SENT, mailbox: "myMailbox" },
            "5": { key: "5", imapName: DEFAULT_FOLDER_NAMES.TRASH, mailbox: "myMailbox" },
            "6": { key: "6", imapName: DEFAULT_FOLDER_NAMES.DRAFTS, mailbox: "myMailbox" }
        };

        const mockedGetters = { MY_MAILBOX_KEY: "myMailbox" };
        expect(getters.MY_INBOX(state, mockedGetters).key).toEqual("2");
        expect(getters.MY_OUTBOX(state, mockedGetters).key).toEqual("3");
        expect(getters.MY_DRAFTS(state, mockedGetters).key).toEqual("6");
        expect(getters.MY_SENT(state, mockedGetters).key).toEqual("4");
        expect(getters.MY_TRASH(state, mockedGetters).key).toEqual("5");
    });
});
