import mutations from "../mutations";
import { SET_UNREAD_COUNT, RENAME_FOLDER, REMOVE_FOLDER, ADD_FOLDERS, ADD_FOLDER } from "~mutations";

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

    describe("ADD_FOLDERS", () => {
        const { [ADD_FOLDERS]: addFolders } = mutations;
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
            addFolders(state, folders);
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
            addFolders(state, folders);
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
            removeFolder(state, "123");
            expect(state).toEqual({});
        });
        test("delete non existing key", () => {
            const state = {
                "123": {},
                "666": {}
            };
            const initialState = JSON.parse(JSON.stringify(state));
            removeFolder(state, "42");
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
            setUnreadCount(state, { key: "123", count: 2 });
            expect(state["123"].unread).toEqual(2);
        });
        test("set unread count", () => {
            const state = {
                "123": {
                    unread: 0
                }
            };
            setUnreadCount(state, { key: "123", count: 2 });
            expect(state["123"].unread).toEqual(2);
        });

        test("no negative value", () => {
            const state = {
                "123": {
                    unread: 2
                }
            };
            setUnreadCount(state, { key: "123", count: -5 });
            expect(state["123"].unread).toEqual(2);
        });
    });

    describe("TOGGLE_FOLDER", () => {
        const toggleFolder = mutations.TOGGLE_FOLDER;
        test("toggle 'expanded' value", () => {
            const state = {
                "123": {
                    expanded: true
                }
            };
            expect(state["123"].expanded).toEqual(true);
            toggleFolder(state, "123");
            expect(state["123"].expanded).toEqual(false);
            toggleFolder(state, "123");
            expect(state["123"].expanded).toEqual(true);
        });

        test("default 'expanded' value is false", () => {
            const state = {
                "123": {}
            };
            expect(state["123"].expanded).toBeFalsy();
            toggleFolder(state, "123");
            expect(state["123"].expanded).toEqual(true);
        });
    });
});
