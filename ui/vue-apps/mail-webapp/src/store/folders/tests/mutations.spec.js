import mutations from "../mutations";
import {
    ADD_FLAG,
    SET_MAILBOX_FOLDERS,
    ADD_FOLDER,
    MOVE_MESSAGES,
    RENAME_FOLDER,
    DELETE_FLAG,
    REMOVE_FOLDER,
    REMOVE_MESSAGES,
    SET_FOLDER_EXPANDED,
    SET_UNREAD_COUNT
} from "~mutations";
import { Flag } from "@bluemind/email";

describe("folder mutations", () => {
    describe("ADD_FOLDER", () => {
        const { [ADD_FOLDER]: addFolder } = mutations;

        test("add folder to empty state", () => {
            const folder = {
                key: "123"
            };
            const state = {};
            addFolder(state, folder);
            expect(state).toEqual({
                [folder.key]: folder
            });
        });
        test("add folder with already existing key", () => {
            const folder = {
                key: "123",
                newProp: "newProp"
            };
            const state = {
                "123": {
                    key: "123"
                }
            };
            addFolder(state, folder);
            expect(state).toEqual({
                [folder.key]: folder
            });
        });
    });

    describe("SET_MAILBOX_FOLDERS", () => {
        const { [SET_MAILBOX_FOLDERS]: addFolders } = mutations;
        test("add folders to empty state", () => {
            const state = {};
            const folders = [
                {
                    key: "123"
                },
                {
                    key: "666"
                }
            ];
            addFolders(state, { folders });
            expect(state).toEqual(
                folders.reduce((acc, folder) => {
                    return {
                        ...acc,
                        [folder.key]: folder
                    };
                }, {})
            );
        });

        test("add folders with some keys already existing", () => {
            const state = {
                "123": {
                    key: "123",
                    oldProp: "oldProp"
                }
            };
            const folders = [
                {
                    key: "123",
                    oldProp: "newValue"
                },
                {
                    key: "666"
                }
            ];
            addFolders(state, { folders });
            expect(state).toEqual(
                folders.reduce((acc, folder) => {
                    return {
                        ...acc,
                        [folder.key]: folder
                    };
                }, {})
            );
        });
    });

    describe("DELETE_FOLDER", () => {
        const { [REMOVE_FOLDER]: removeFolder } = mutations;
        test("delete one folder by key", () => {
            const state = {
                "123": {}
            };
            removeFolder(state, { key: "123" });
            expect(state).toEqual({});
        });
        test("delete non existing key", () => {
            const state = {
                "123": {},
                "666": {}
            };
            const initialState = JSON.parse(JSON.stringify(state));
            removeFolder(state, { key: "42" });
            expect(state).toEqual(initialState);
        });
    });

    describe("RENAME_FOLDER", () => {
        const { [RENAME_FOLDER]: renameFolder } = mutations;
        test("rename an existing folder with name", () => {
            const state = {
                "123": {
                    name: "foo",
                    path: "foo"
                }
            };
            renameFolder(state, { key: "123", name: "foobar", path: "foobar" });
            expect(state["123"]).toEqual({ name: "foobar", path: "foobar" });
        });

        test("rename a folder only change the name and the path property", () => {
            const state = {
                "123": {
                    aprop: "bar",
                    name: "foo",
                    path: "foo",
                    otherprop: "bar"
                }
            };
            renameFolder(state, { key: "123", name: "foobar", path: "foobar" });
            expect(state["123"]).toEqual({
                aprop: "bar",
                name: "foobar",
                path: "foobar",
                otherprop: "bar"
            });
        });
    });

    describe("UNREAD_COUNT", () => {
        const { [SET_UNREAD_COUNT]: setUnreadCount } = mutations;
        test("with no initial property", () => {
            const state = {
                "123": {}
            };
            setUnreadCount(state, { key: "123", unread: 2 });
            expect(state["123"].unread).toEqual(2);
        });
        test("set unread count", () => {
            const state = {
                "123": {
                    unread: 0
                }
            };
            setUnreadCount(state, { key: "123", unread: 2 });
            expect(state["123"].unread).toEqual(2);
        });
    });

    describe("SET_FOLDER_EXPANDED", () => {
        const setFolderExpanded = mutations[SET_FOLDER_EXPANDED];
        test("set 'expanded' value", () => {
            const state = {
                "123": {
                    expanded: true
                }
            };
            expect(state["123"].expanded).toEqual(true);
            setFolderExpanded(state, { key: "123", expanded: false });
            expect(state["123"].expanded).toEqual(false);
            setFolderExpanded(state, { key: "123", expanded: true });
            expect(state["123"].expanded).toEqual(true);
        });
    });

    describe("REMOVE_MESSAGES", () => {
        const UNREAD_MESSAGE = { date: 1, flags: [], folderRef: { key: "123" } };
        const READ_MESSAGE = { date: 1, flags: [Flag.SEEN], folderRef: { key: "123" } };
        const UNLOADED_MESSAGE = { date: undefined, flags: [], folderRef: { key: "123" } };
        test("Set unread count on remove unread", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [UNREAD_MESSAGE];
            mutations[REMOVE_MESSAGES](state, messages);
            expect(state["123"].unread).toEqual(4);
        });
        test("Do not set unread count on remove read", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [READ_MESSAGE];
            mutations[REMOVE_MESSAGES](state, messages);
            expect(state["123"].unread).toEqual(5);
        });
        test("Do not set unread count on remove not loaded", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [UNLOADED_MESSAGE];
            mutations[REMOVE_MESSAGES](state, messages);
            expect(state["123"].unread).toEqual(5);
        });
    });
    describe("MOVE_MESSAGES", () => {
        const UNREAD_MESSAGE = { date: 1, flags: [], folderRef: { key: "123" } };
        const READ_MESSAGE = { date: 1, flags: [Flag.SEEN], folderRef: { key: "123" } };
        const UNLOADED_MESSAGE = { date: undefined, flags: [], folderRef: { key: "123" } };
        test("Set unread count on move unread", () => {
            const state = {
                "123": {
                    unread: 5
                },
                "456": {
                    unread: 5
                }
            };
            const messages = [UNREAD_MESSAGE];
            mutations[MOVE_MESSAGES](state, { messages, folder: { key: "456" } });
            expect(state["456"].unread).toEqual(6);
            expect(state["123"].unread).toEqual(4);
        });
        test("Do not set unread count on move read", () => {
            const state = {
                "123": {
                    unread: 5
                },
                "456": {
                    unread: 5
                }
            };
            const messages = [READ_MESSAGE];
            mutations[MOVE_MESSAGES](state, { messages, folder: { key: "456" } });
            expect(state["456"].unread).toEqual(5);
            expect(state["123"].unread).toEqual(5);
        });
        test("Do not set unread count on move not loaded", () => {
            const state = {
                "123": {
                    unread: 5
                },
                "456": {
                    unread: 5
                }
            };
            const messages = [UNLOADED_MESSAGE];
            mutations[MOVE_MESSAGES](state, { messages, folder: { key: "456" } });
            expect(state["456"].unread).toEqual(5);
            expect(state["123"].unread).toEqual(5);
        });
    });
    describe("ADD_FLAG", () => {
        const message = { date: 1, folderRef: { key: "123" } };
        test("Decrease unread count when adding seen flag", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [message];
            mutations[ADD_FLAG](state, { messages, flag: Flag.SEEN });
            expect(state["123"].unread).toEqual(4);
        });
        test("Do not decrease if it's another flag", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [message];
            mutations[ADD_FLAG](state, { messages, flag: Flag.FLAGGED });
            expect(state["123"].unread).toEqual(5);
        });
    });
    describe("REMOVE_FLAG", () => {
        const message = { date: 1, folderRef: { key: "123" } };
        test("Increase unread count when adding seen flag", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [message];
            mutations[DELETE_FLAG](state, { messages, flag: Flag.SEEN });
            expect(state["123"].unread).toEqual(6);
        });
        test("Do not increase if it's another flag", () => {
            const state = {
                "123": {
                    unread: 5
                }
            };
            const messages = [message];
            mutations[DELETE_FLAG](state, { messages, flag: Flag.FLAGGED });
            expect(state["123"].unread).toEqual(5);
        });
    });
});
