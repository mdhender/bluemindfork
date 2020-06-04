import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockMailboxFoldersClient } from "@bluemind/test-mocks";
import { FolderAdaptor, MailboxAdaptor } from "../src";
import {
    state,
    mutations,
    actions,
    ADD_FOLDER,
    ADD_FOLDERS,
    FETCH_FOLDERS,
    CREATE_FOLDER,
    RENAME_FOLDER,
    REMOVE_FOLDER
} from "../src/folders";
import aliceFolders from "./data/users/alice/folders";
import aliceContainers from "./data/users/alice/containers";
import { MailboxType } from "../src/helpers/MailboxAdaptor";

const foldersService = new MockMailboxFoldersClient();
inject.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });
Vue.use(Vuex);

describe("folders store", () => {
    describe("mutations", () => {
        describe("CREATE_FOLDER", () => {
            const { [CREATE_FOLDER]: createFolder } = mutations;

            test("create root folder ", () => {
                const stateForTest = { folders: {} };
                createFolder(stateForTest, {
                    name: "folder",
                    key: "123",
                    parent: null,
                    mailbox: { type: MailboxType.USER, key: "mailbox" }
                });
                expect(stateForTest.folders).toMatchInlineSnapshot(`
                    Object {
                      "123": Object {
                        "default": false,
                        "id": null,
                        "key": "123",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": null,
                        "path": "folder",
                        "uid": null,
                        "writable": undefined,
                      },
                    }
                `);
            });

            test("create sub folder ", () => {
                const stateForTest = { folders: { "123": { key: "123", path: "root" } } };
                createFolder(stateForTest, {
                    name: "folder",
                    key: "456",
                    parent: "123",
                    mailbox: { type: MailboxType.USER, key: "mailbox" }
                });
                expect(stateForTest.folders).toMatchInlineSnapshot(`
                    Object {
                      "123": Object {
                        "key": "123",
                        "path": "root",
                      },
                      "456": Object {
                        "default": false,
                        "id": null,
                        "key": "456",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": "123",
                        "path": "root/folder",
                        "uid": null,
                        "writable": undefined,
                      },
                    }
                `);
            });

            test("folder inherit mailbox writable ", () => {
                const stateStateForTest = { folders: {} };
                createFolder(stateStateForTest, {
                    name: "folder",
                    key: "123",
                    parent: null,
                    mailbox: { type: MailboxType.USER, key: "mailbox", writable: true }
                });
                expect(stateStateForTest.folders).toMatchInlineSnapshot(`
                    Object {
                      "123": Object {
                        "default": false,
                        "id": null,
                        "key": "123",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": null,
                        "path": "folder",
                        "uid": null,
                        "writable": true,
                      },
                    }
                `);
            });
        });
        describe("ADD_FOLDER", () => {
            const { [ADD_FOLDER]: addFolder } = mutations;

            test("add folder to empty state", () => {
                const folder = {
                    key: "123"
                };
                const stateForTest = { folders: {} };
                addFolder(stateForTest, folder);
                expect(stateForTest.folders).toStrictEqual({
                    [folder.key]: { ...folder }
                });
            });
            test("add folder with already existing key", () => {
                const { [ADD_FOLDER]: addFolder } = mutations;
                const folder = {
                    key: "123",
                    newProp: "newProp"
                };
                const stateForTest = {
                    folders: {
                        "123": {
                            key: "123"
                        }
                    }
                };
                addFolder(stateForTest, folder);
                expect(stateForTest.folders).toStrictEqual({
                    [folder.key]: folder
                });
            });
        });
        describe("ADD_FOLDERS", () => {
            const { [ADD_FOLDERS]: addFolders } = mutations;
            test("add folders to empty state", () => {
                const stateForTest = { folders: {} };
                const folders = [
                    {
                        key: "123"
                    },
                    {
                        key: "666"
                    }
                ];
                addFolders(stateForTest, folders);
                expect(stateForTest.folders).toStrictEqual(
                    folders.reduce((acc, folder) => {
                        return {
                            ...acc,
                            [folder.key]: folder
                        };
                    }, {})
                );
            });

            test("add folders with some keys already existing", () => {
                const stateForTest = {
                    folders: {
                        "123": {
                            key: "123",
                            oldProp: "oldProp"
                        }
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
                addFolders(stateForTest, folders);
                expect(stateForTest.folders).toStrictEqual(
                    folders.reduce((acc, folder) => {
                        return {
                            ...acc,
                            [folder.key]: folder
                        };
                    }, {})
                );
            });
        });
        describe("REMOVE_FOLDER", () => {
            const { [REMOVE_FOLDER]: removeFolder } = mutations;
            test("remove one folder by key", () => {
                const stateForTest = {
                    folders: {}
                };
                removeFolder(stateForTest, "123");
                expect(stateForTest.folders).toStrictEqual({});
            });
            test("remove non existing key", () => {
                const stateForTest = {
                    folders: {
                        "123": {},
                        "666": {}
                    }
                };
                removeFolder(stateForTest, "42");
                expect(stateForTest.folders).toStrictEqual({
                    "123": {},
                    "666": {}
                });
            });
        });
        describe("RENAME_FOLDER", () => {
            const { [RENAME_FOLDER]: renameFolder } = mutations;
            test("rename an existing folder with name", () => {
                const stateForTest = {
                    folders: {
                        "123": {
                            name: "foo",
                            path: "foo"
                        }
                    }
                };
                renameFolder(stateForTest, { key: "123", name: "foobar" });
                expect(stateForTest.folders["123"]).toStrictEqual({ name: "foobar", path: "foobar" });
            });

            test("rename a folder only change the name property", () => {
                const stateForTest = {
                    folders: {
                        "123": {
                            aprop: "bar",
                            name: "foo",
                            path: "foo",
                            otherprop: "bar"
                        }
                    }
                };
                renameFolder(stateForTest, { key: "123", name: "foobar" });
                expect(stateForTest.folders["123"]).toStrictEqual({
                    aprop: "bar",
                    name: "foobar",
                    path: "foobar",
                    otherprop: "bar"
                });
            });
        });
    });

    describe("actions", () => {
        let store;
        beforeEach(() => {
            store = new Vuex.Store(cloneDeep({ state, mutations, actions }));
            foldersService.all.mockClear();
        });
        describe("FETCH_FOLDERS", () => {
            test("Store folders in state", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                foldersService.all.mockResolvedValueOnce(aliceFolders);
                await store.dispatch(FETCH_FOLDERS, mailbox);
                expect(Object.keys(store.state.folders).length).toEqual(aliceFolders.length);
                aliceFolders.forEach(item => {
                    const folder = FolderAdaptor.fromMailboxFolder(item, mailbox);
                    expect(folder).toEqual(store.state.folders[folder.key]);
                    if (folder.parent) {
                        expect(store.state.folders[folder.parent]).toBeDefined();
                    }
                });
            });
        });
        describe("CREATE_FOLDER", () => {
            test("create a folder in an empty state", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                foldersService.createBasic.mockResolvedValueOnce({ uid: "truc", id: 2 });
                await store.dispatch(CREATE_FOLDER, { key: "folder-1", name: "New Folder", parent: null, mailbox });
                expect(store.state.folders).toMatchInlineSnapshot(`
                    Object {
                      "folder-1": Object {
                        "default": false,
                        "id": 2,
                        "key": "folder-1",
                        "mailbox": "user.2017",
                        "name": "New Folder",
                        "parent": null,
                        "path": "New Folder",
                        "uid": "truc",
                        "writable": true,
                      },
                    }
                `);
            });

            test("create a folder in filled state", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                store.state.folders = { "folder-2": {} };
                foldersService.createBasic.mockResolvedValueOnce({ uid: "truc", id: 2 });
                await store.dispatch(CREATE_FOLDER, { key: "folder-1", name: "New Folder", parent: null, mailbox });
                expect(store.state.folders).toMatchInlineSnapshot(`
                    Object {
                      "folder-1": Object {
                        "default": false,
                        "id": 2,
                        "key": "folder-1",
                        "mailbox": "user.2017",
                        "name": "New Folder",
                        "parent": null,
                        "path": "New Folder",
                        "uid": "truc",
                        "writable": true,
                      },
                      "folder-2": Object {},
                    }
                `);
            });

            test("fail to create a folder", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                foldersService.createBasic.mockRejectedValueOnce("Server Error");
                expect.assertions(2);
                try {
                    await store.dispatch(CREATE_FOLDER, { key: "folder-1", name: "New Folder", parent: null, mailbox });
                } catch (error) {
                    expect(error).toBeDefined();
                }
                expect(Object.keys(store.state.folders).length).toBe(0);
            });
        });
        describe("RENAME_FOLDER", () => {
            test("RENAME_FOLDER action rename a folder using optimistic rendering", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                store.state.folders["folder-1"] = {
                    key: "folder-1",
                    name: "Old name",
                    path: "Old name"
                };
                store.dispatch(RENAME_FOLDER, { mailbox, key: "folder-1", name: "New name" });
                expect(Object.values(store.state.folders).pop().name).toBe("New name");
                expect(Object.values(store.state.folders).pop().path).toBe("New name");
            });

            test("RENAME_FOLDER failure to restore old folder name", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                store.state.folders["folder-1"] = {
                    key: "folder-1",
                    name: "Old name"
                };
                foldersService.updateById.mockRejectedValueOnce("Server Error");
                expect.assertions(2);
                try {
                    await store.dispatch(RENAME_FOLDER, { mailbox, key: "folder-1", name: "New name" });
                } catch (error) {
                    expect(error).toBeDefined();
                }
                expect(Object.values(store.state.folders).pop().name).toBe("Old name");
            });
        });
        describe("RENAME_FOLDER", () => {
            test("REMOVE_FOLDER action remove a folder using optimistic rendering", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                const folder = { key: "folder-1" };
                store.state.folders["folder-1"] = cloneDeep(folder);
                store.state.folders["folder-2"] = cloneDeep(folder);

                store.dispatch(REMOVE_FOLDER, { key: "folder-1", mailbox });
                expect(Object.keys(store.state.folders)).toEqual(["folder-2"]);
            });
            test("REMOVE_FOLDER failure restore removed folder", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                const folder = { key: "folder-1", name: "name" };
                store.state.folders["folder-1"] = cloneDeep(folder);
                foldersService.deepDelete.mockRejectedValueOnce("Server Error");
                expect.assertions(2);
                try {
                    await store.dispatch(REMOVE_FOLDER, { key: "folder-1", mailbox });
                } catch (error) {
                    expect(error).toBeDefined();
                }
                expect(store.state.folders["folder-1"]).toEqual(folder);
            });
        });
    });
});
