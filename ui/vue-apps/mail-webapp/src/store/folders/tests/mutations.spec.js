import mutations from "../mutations";
import {
    ADD_FLAG,
    SET_MAILBOX_FOLDERS,
    ADD_FOLDER,
    DELETE_FLAG,
    REMOVE_FOLDER,
    SET_FOLDER_EXPANDED,
    SET_UNREAD_COUNT,
    UPDATE_FOLDER,
    UPDATE_PATHS
} from "~/mutations";
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

    describe("UPDATE_FOLDER", () => {
        const { [UPDATE_FOLDER]: updateFolder } = mutations;
        test("update an existing folder with name", () => {
            const state = {
                "123": {
                    path: "foo",
                    parent: null
                }
            };
            updateFolder(state, { key: "123", path: "foobar", parent: "foobar" });
            expect(state["123"]).toEqual({ path: "foobar", parent: "foobar" });
        });

        test("update a folder only change the parent and the path property", () => {
            const state = {
                "123": {
                    aprop: "bar",
                    parent: "foo",
                    path: "foo",
                    otherprop: "bar"
                }
            };
            updateFolder(state, { key: "123", parent: "foobar", path: "foobar" });
            expect(state["123"]).toEqual({
                aprop: "bar",
                parent: "foobar",
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
    describe("UPDATE_PATHS", () => {
        const { [UPDATE_PATHS]: updatePaths } = mutations;
        test("update paths", () => {
            const state = {
                "0": { path: "a" },
                "1": { path: "a/b/" },
                "2": { path: "a/b/c" },
                "3": { path: "a/b/c/d" },
                "4": { path: "a/b/c/d/e" },
                "5": { path: "a/b/c/d/e/f" },
                "6": { path: "z/y" },
                "7": { path: "z/y/x" }
            };
            const folders = [{ key: "2" }, { key: "3" }, { key: "4" }, { key: "7" }];
            const initial = { path: "a/b/c" };
            const updated = { path: "new/parent/c" };

            updatePaths(state, { folders, initial, updated });
            expect(state).toEqual({
                "0": { path: "a" },
                "1": { path: "a/b/" },
                "2": { path: "new/parent/c" },
                "3": { path: "new/parent/c/d" },
                "4": { path: "new/parent/c/d/e" },
                "5": { path: "a/b/c/d/e/f" },
                "6": { path: "z/y" },
                "7": { path: "z/y/x" }
            });
        });
    });
});
