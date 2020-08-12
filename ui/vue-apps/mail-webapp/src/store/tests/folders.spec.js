import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockMailboxFoldersClient, MockI18NProvider } from "@bluemind/test-utils";
import { DEFAULT_FOLDER_NAMES } from "../helpers/DefaultFolders";
import { FolderAdaptor } from "../helpers/FolderAdaptor";
import { MailboxAdaptor, MailboxType } from "../helpers/MailboxAdaptor";
import { state, mutations, actions, getters } from "../folders";
import aliceFolders from "./data/users/alice/folders";
import aliceContainers from "./data/users/alice/containers";

const foldersService = new MockMailboxFoldersClient();
inject.register({ provide: "MailboxFoldersPersistence", factory: () => foldersService });
inject.register({ provide: "i18n", factory: () => MockI18NProvider });
Vue.use(Vuex);

describe("folders store", () => {
    describe("mutations", () => {
        describe("CREATE_FOLDER", () => {
            test("create root folder ", () => {
                const stateForTest = { folders: {} };
                mutations.CREATE_FOLDER(stateForTest, {
                    name: "folder",
                    key: "123",
                    parent: null,
                    mailbox: { type: MailboxType.USER, key: "mailbox" }
                });
                expect(stateForTest.folders).toMatchInlineSnapshot(`
                    Object {
                      "123": Object {
                        "default": false,
                        "expanded": false,
                        "id": null,
                        "imapName": "folder",
                        "key": "123",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": null,
                        "path": "folder",
                        "uid": null,
                        "unread": 0,
                        "writable": undefined,
                      },
                    }
                `);
            });

            test("create sub folder ", () => {
                const stateForTest = { folders: { "123": { key: "123", path: "root" } } };
                mutations.CREATE_FOLDER(stateForTest, {
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
                        "expanded": false,
                        "id": null,
                        "imapName": "folder",
                        "key": "456",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": "123",
                        "path": "root/folder",
                        "uid": null,
                        "unread": 0,
                        "writable": undefined,
                      },
                    }
                `);
            });

            test("folder inherit mailbox writable ", () => {
                const stateStateForTest = { folders: {} };
                mutations.CREATE_FOLDER(stateStateForTest, {
                    name: "folder",
                    key: "123",
                    parent: null,
                    mailbox: { type: MailboxType.USER, key: "mailbox", writable: true }
                });
                expect(stateStateForTest.folders).toMatchInlineSnapshot(`
                    Object {
                      "123": Object {
                        "default": false,
                        "expanded": false,
                        "id": null,
                        "imapName": "folder",
                        "key": "123",
                        "mailbox": "mailbox",
                        "name": "folder",
                        "parent": null,
                        "path": "folder",
                        "uid": null,
                        "unread": 0,
                        "writable": true,
                      },
                    }
                `);
            });
        });
        describe("ADD_FOLDER", () => {
            test("add folder to empty state", () => {
                const folder = {
                    key: "123"
                };
                const stateForTest = { folders: {} };
                mutations.ADD_FOLDER(stateForTest, folder);
                expect(stateForTest.folders).toStrictEqual({
                    [folder.key]: { ...folder }
                });
            });
            test("add folder with already existing key", () => {
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
                mutations.ADD_FOLDER(stateForTest, folder);
                expect(stateForTest.folders).toStrictEqual({
                    [folder.key]: folder
                });
            });
        });
        describe("ADD_FOLDERS", () => {
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
                mutations.ADD_FOLDERS(stateForTest, folders);
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
                mutations.ADD_FOLDERS(stateForTest, folders);
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
            test("remove one folder by key", () => {
                const stateForTest = {
                    folders: {}
                };
                mutations.REMOVE_FOLDER(stateForTest, "123");
                expect(stateForTest.folders).toStrictEqual({});
            });
            test("remove non existing key", () => {
                const stateForTest = {
                    folders: {
                        "123": {},
                        "666": {}
                    }
                };
                mutations.REMOVE_FOLDER(stateForTest, "42");
                expect(stateForTest.folders).toStrictEqual({
                    "123": {},
                    "666": {}
                });
            });
        });
        describe("RENAME_FOLDER", () => {
            test("rename an existing folder with name", () => {
                const stateForTest = {
                    folders: {
                        "123": {
                            name: "foo",
                            path: "foo"
                        }
                    }
                };
                mutations.RENAME_FOLDER(stateForTest, { key: "123", name: "foobar" });
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
                mutations.RENAME_FOLDER(stateForTest, { key: "123", name: "foobar" });
                expect(stateForTest.folders["123"]).toStrictEqual({
                    aprop: "bar",
                    name: "foobar",
                    path: "foobar",
                    otherprop: "bar"
                });
            });
        });

        test("set unread count", () => {
            const stateForTest = {
                folders: {
                    "123": {
                        unread: 0
                    }
                }
            };
            mutations.SET_UNREAD_COUNT(stateForTest, { key: "123", count: 2 });
            expect(stateForTest.folders["123"].unread).toEqual(2);

            mutations.SET_UNREAD_COUNT(stateForTest, { key: "123", count: -5 });
            expect(stateForTest.folders["123"].unread).toEqual(2);
        });

        test("toggle folder", () => {
            const stateForTest = {
                folders: {
                    "1": {
                        expanded: false
                    },
                    "2": {}
                }
            };
            mutations.TOGGLE_FOLDER(stateForTest, "1");
            expect(stateForTest.folders["1"].expanded).toEqual(true);
            mutations.TOGGLE_FOLDER(stateForTest, "1");
            expect(stateForTest.folders["1"].expanded).toEqual(false);

            mutations.TOGGLE_FOLDER(stateForTest, "2");
            expect(stateForTest.folders["2"].expanded).toEqual(true);
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
                await store.dispatch("FETCH_FOLDERS", mailbox);
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
                await store.dispatch("CREATE_FOLDER", { key: "folder-1", name: "New Folder", parent: null, mailbox });
                expect(store.state.folders).toMatchInlineSnapshot(`
                    Object {
                      "folder-1": Object {
                        "default": false,
                        "expanded": false,
                        "id": 2,
                        "imapName": "New Folder",
                        "key": "folder-1",
                        "mailbox": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                        "name": "New Folder",
                        "parent": null,
                        "path": "New Folder",
                        "uid": "truc",
                        "unread": 0,
                        "writable": true,
                      },
                    }
                `);
            });

            test("create a folder in filled state", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                store.state.folders = { "folder-2": {} };
                foldersService.createBasic.mockResolvedValueOnce({ uid: "truc", id: 2 });
                await store.dispatch("CREATE_FOLDER", { key: "folder-1", name: "New Folder", parent: null, mailbox });
                expect(store.state.folders).toMatchInlineSnapshot(`
                    Object {
                      "folder-1": Object {
                        "default": false,
                        "expanded": false,
                        "id": 2,
                        "imapName": "New Folder",
                        "key": "folder-1",
                        "mailbox": "user.6793466E-F5D4-490F-97BF-DF09D3327BF4",
                        "name": "New Folder",
                        "parent": null,
                        "path": "New Folder",
                        "uid": "truc",
                        "unread": 0,
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
                    await store.dispatch("CREATE_FOLDER", {
                        key: "folder-1",
                        name: "New Folder",
                        parent: null,
                        mailbox
                    });
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
                store.dispatch("RENAME_FOLDER", { mailbox, key: "folder-1", name: "New name" });
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
                    await store.dispatch("RENAME_FOLDER", { mailbox, key: "folder-1", name: "New name" });
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

                store.dispatch("REMOVE_FOLDER", { key: "folder-1", mailbox });
                expect(Object.keys(store.state.folders)).toEqual(["folder-2"]);
            });
            test("REMOVE_FOLDER failure restore removed folder", async () => {
                const mailbox = MailboxAdaptor.fromMailboxContainer(aliceContainers[0]);
                const folder = { key: "folder-1", name: "name" };
                store.state.folders["folder-1"] = cloneDeep(folder);
                foldersService.deepDelete.mockRejectedValueOnce("Server Error");
                expect.assertions(2);
                try {
                    await store.dispatch("REMOVE_FOLDER", { key: "folder-1", mailbox });
                } catch (error) {
                    expect(error).toBeDefined();
                }
                expect(store.state.folders["folder-1"]).toEqual(folder);
            });
        });
    });

    describe("getters", () => {
        let store;
        beforeEach(() => {
            store = new Vuex.Store(cloneDeep({ state, mutations, actions, getters }));
            foldersService.all.mockClear();
        });

        test("FOLDER_BY_PATH", async () => {
            const testFolder = {
                name: "foo",
                path: "foo",
                key: "123"
            };
            store.state.folders = {
                "123": testFolder
            };
            expect(store.getters["FOLDER_BY_PATH"]("foo")).toEqual(testFolder);
            expect(store.getters["FOLDER_BY_PATH"]("whatever")).toEqual(undefined);
        });

        test("FOLDER_BY_PATH", async () => {
            const testFolder = {
                name: "foo",
                path: "foo",
                key: "123"
            };
            store.state.folders = {
                "123": testFolder
            };
            expect(store.getters["FOLDER_BY_PATH"]("foo")).toEqual(testFolder);
            expect(store.getters["FOLDER_BY_PATH"]("whatever")).toEqual(undefined);
        });

        test("FOLDER_BY_MAILBOX", async () => {
            store.state.folders = {
                "1": {
                    key: "1",
                    mailbox: "mine"
                },
                "2": {
                    key: "2",
                    mailbox: "mine"
                },
                "3": {
                    key: "3",
                    mailbox: "other"
                }
            };
            expect(store.getters["FOLDER_BY_MAILBOX"]("mine")).toEqual([
                { key: "1", mailbox: "mine" },
                { key: "2", mailbox: "mine" }
            ]);
            expect(store.getters["FOLDER_BY_MAILBOX"]("other")).toEqual([{ key: "3", mailbox: "other" }]);
        });

        test("HAS_CHILDREN_GETTER", async () => {
            store.state.folders = {
                "1": {
                    key: "1",
                    parent: null
                },
                "2": {
                    key: "2",
                    parent: "1"
                },
                "3": {
                    key: "3",
                    parent: "unknown"
                }
            };
            expect(store.getters["HAS_CHILDREN_GETTER"]("1")).toEqual(true);
            expect(store.getters["HAS_CHILDREN_GETTER"]("2")).toEqual(false);
            expect(store.getters["HAS_CHILDREN_GETTER"]("3")).toEqual(false);
        });

        test("MAILSHARE_FOLDERS", async () => {
            const state = {
                folders: {
                    "1": {
                        key: "1",
                        mailbox: "A"
                    },
                    "2": {
                        key: "2",
                        mailbox: "unknown"
                    },
                    "3": {
                        key: "3",
                        mailbox: "B"
                    },
                    "4": {
                        key: "4",
                        mailbox: "C"
                    }
                }
            };
            const mockedGetters = { MAILSHARE_KEYS: ["A", "C"] };
            expect(getters["MAILSHARE_FOLDERS"](state, mockedGetters)).toEqual(["1", "4"]);
        });

        test("MY_MAILBOX_FOLDERS", async () => {
            const state = {
                folders: {
                    "1": {
                        key: "1",
                        mailbox: "A"
                    },
                    "2": {
                        key: "2",
                        mailbox: "unknown"
                    },
                    "3": {
                        key: "3",
                        mailbox: "B"
                    },
                    "4": {
                        key: "4",
                        mailbox: "C"
                    }
                }
            };
            const mockedGetters = { MY_MAILBOX_KEY: "B" };
            expect(getters["MY_MAILBOX_FOLDERS"](state, mockedGetters)).toEqual(["3"]);
        });

        test("DEFAULT FOLDERS", async () => {
            const state = {};
            const folders = {
                "1": {
                    key: "1",
                    imapName: "whatever",
                    mailbox: "myMailbox"
                },
                "1bis": {
                    key: "1bis",
                    imapName: DEFAULT_FOLDER_NAMES.INBOX,
                    mailbox: "other"
                },
                "2": {
                    key: "2",
                    imapName: DEFAULT_FOLDER_NAMES.INBOX,
                    mailbox: "myMailbox"
                },
                "3": {
                    key: "3",
                    imapName: DEFAULT_FOLDER_NAMES.OUTBOX,
                    mailbox: "myMailbox"
                },
                "4": {
                    key: "4",
                    imapName: DEFAULT_FOLDER_NAMES.SENT,
                    mailbox: "myMailbox"
                },
                "5": {
                    key: "5",
                    imapName: DEFAULT_FOLDER_NAMES.TRASH,
                    mailbox: "myMailbox"
                },
                "6": {
                    key: "6",
                    imapName: DEFAULT_FOLDER_NAMES.DRAFTS,
                    mailbox: "myMailbox"
                }
            };
            state.folders = folders;

            const mockedGetters = { MY_MAILBOX_KEY: "myMailbox" };
            expect(getters["MY_INBOX"](state, mockedGetters).key).toEqual("2");
            expect(getters["MY_OUTBOX"](state, mockedGetters).key).toEqual("3");
            expect(getters["MY_DRAFTS"](state, mockedGetters).key).toEqual("6");
            expect(getters["MY_SENT"](state, mockedGetters).key).toEqual("4");
            expect(getters["MY_TRASH"](state, mockedGetters).key).toEqual("5");
        });
    });
});
