import Vue from "vue";
import Vuex from "vuex";
import deepClone from "lodash.clonedeep";
import api from "../../api/apiFolders";
import storeConfig from "../index";
import {
    FETCH_FOLDERS,
    CREATE_FOLDER,
    EMPTY_FOLDER,
    MARK_FOLDER_AS_READ,
    MOVE_FOLDER,
    REMOVE_FOLDER,
    RENAME_FOLDER,
    UNREAD_FOLDER_COUNT
} from "~/actions";
import { FOLDER_BY_PATH } from "~/getters";
import { ADD_FOLDER } from "~/mutations";
import injector from "@bluemind/inject";

Vue.use(Vuex);
jest.mock("../../api/apiFolders");

injector.register({
    provide: "i18n",
    use: {
        t: jest.fn()
    }
});

describe("actions", () => {
    let store;
    beforeEach(() => {
        storeConfig.actions["alert/LOADING"] = jest.fn();
        storeConfig.actions["alert/SUCCESS"] = jest.fn();
        storeConfig.actions["alert/ERROR"] = jest.fn();
        api.createNewFolder.mockClear();
        api.updateFolder.mockClear();
        api.getAllFolders.mockClear();
        storeConfig.actions["alert/LOADING"] = jest.fn();
        storeConfig.actions["alert/SUCCESS"] = jest.fn();
        storeConfig.actions["alert/ERROR"] = jest.fn();
        storeConfig.getters[FOLDER_BY_PATH] = jest.fn(() => path => path.match(/\/foo$/) || path.match(/\/foo \(1\)$/));
        store = new Vuex.Store(deepClone(storeConfig));
    });
    describe("FETCH_FOLDERS", () => {
        test("Fetch folders", async () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            api.getAllFolders.mockReturnValue(require("../../tests/data/users/alice/folders.json"));
            await store.dispatch(FETCH_FOLDERS, mailbox);
            expect(store.state).toMatchSnapshot();
        });
    });
    describe("CREATE_FOLDER", () => {
        test("Create folder", async () => {
            const mailbox = {
                type: "users",
                name: "",
                remoteRef: {}
            };
            const folder = { name: "bar", path: "baz" };
            api.createNewFolder.mockResolvedValue({ id: 1, uid: "bar-baz" });
            await store.dispatch(CREATE_FOLDER, { ...folder, mailbox });
            expect(api.createNewFolder).toHaveBeenCalledWith(mailbox, {
                internalId: null,
                uid: null,
                value: {
                    fullName: "bar",
                    name: "bar",
                    parentUid: null
                }
            });
            expect(Object.keys(store.state)).toHaveLength(1);
            expect(store.state).toMatchSnapshot();
        });
        test("Create folder with children", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            const folder = { name: "foo/bar/baz", path: "foo/bar/baz" };
            api.createNewFolder.mockResolvedValueOnce({ id: 1, uid: "foo-uid" });
            api.createNewFolder.mockResolvedValueOnce({ id: 2, uid: "bar-uid" });
            api.createNewFolder.mockResolvedValueOnce({ id: 3, uid: "baz-uid" });
            await store.dispatch(CREATE_FOLDER, { ...folder, mailbox });
            expect(api.createNewFolder).toHaveBeenNthCalledWith(1, mailbox, {
                internalId: null,
                uid: null,
                value: {
                    fullName: "foo",
                    name: "foo",
                    parentUid: null
                }
            });
            expect(api.createNewFolder).toHaveBeenNthCalledWith(2, mailbox, {
                internalId: null,
                uid: null,
                value: {
                    fullName: "foo/bar",
                    name: "bar",
                    parentUid: "foo-uid"
                }
            });
            expect(api.createNewFolder).toHaveBeenNthCalledWith(3, mailbox, {
                internalId: null,
                uid: null,
                value: {
                    fullName: "foo/bar/baz",
                    name: "baz",
                    parentUid: "bar-uid"
                }
            });
            expect(Object.keys(store.state)).toHaveLength(3);
        });
        test("Create a folder with failure", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            api.createNewFolder.mockRejectedValueOnce(new Error("Mocked rejection"));
            expect.assertions(2);
            try {
                await store.dispatch(CREATE_FOLDER, {
                    ...{ name: "Foo", path: "foo", parent: null },
                    mailbox
                });
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state).toEqual({});
            }
        });
    });
    describe("RENAME_FOLDER", () => {
        test("Rename folder", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            const oldFolder = { key: "1", remoteRef: {}, name: "foo", imapName: "foo", path: "foobaz/foo" };
            const newFolder = { key: "1", remoteRef: {}, name: "bar", imapName: "bar", path: "foobaz/bar" };
            store.commit(ADD_FOLDER, oldFolder);
            await store.dispatch(RENAME_FOLDER, { folder: oldFolder, name: "bar", mailbox });
            expect(api.updateFolder).toHaveBeenCalledWith(mailbox, {
                internalId: undefined,
                uid: "1",
                value: {
                    fullName: "foobaz/bar",
                    name: "bar",
                    parentUid: undefined
                }
            });
            expect(store.state).toEqual({ [newFolder.key]: newFolder });
        });
        test("Rename folder with failure", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };
            const oldFolder = { key: "1", name: "foo", path: "baz", remoteRef: {} };
            api.updateFolder.mockRejectedValueOnce(new Error("Mocked rejection"));
            store.commit(ADD_FOLDER, oldFolder);
            expect.assertions(2);
            try {
                await store.dispatch(RENAME_FOLDER, { folder: oldFolder, name: "bar", mailbox });
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state).toEqual({ [oldFolder.key]: oldFolder });
            }
        });
    });
    describe("MOVE_FOLDER", () => {
        test("move folder", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            const oldFolder = {
                key: "1",
                remoteRef: {},
                name: "bar",
                path: "foobaz/bar",
                parent: "-1",
                mailboxRef: { key: "mboxKey" }
            };
            const newFolder = {
                key: "1",
                remoteRef: {},
                name: "bar",
                path: "foo/bar",
                parent: "2",
                mailboxRef: { key: "mboxKey" }
            };
            const newParent = {
                key: "2",
                remoteRef: { uid: "uid-2" },
                name: "foo",
                path: "foo",
                mailboxRef: { key: "mboxKey" }
            };

            store.commit(ADD_FOLDER, oldFolder);
            store.commit(ADD_FOLDER, newParent);
            await store.dispatch(MOVE_FOLDER, { folder: oldFolder, parent: newParent, mailbox });
            expect(api.updateFolder).toHaveBeenCalledWith(mailbox, {
                internalId: undefined,
                uid: "1",
                value: {
                    fullName: "foo/bar",
                    name: "bar",
                    parentUid: "2"
                }
            });
            expect(store.state[newFolder.key]).toEqual(newFolder);
        });
        test("move folder to existing path will increment its name", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            const oldFolder = {
                key: "1",
                remoteRef: {},
                name: "foo",
                path: "toh/foo",
                parent: "-1",
                mailboxRef: { key: "mboxKey" }
            };
            const existingFolder = {
                key: "3",
                remoteRef: {},
                name: "foo",
                path: "kung/foo",
                parent: "2",
                mailboxRef: { key: "mboxKey" }
            };
            const existingFolder2 = {
                key: "4",
                remoteRef: {},
                name: "foo (1)",
                path: "kung/foo (1)",
                parent: "2",
                mailboxRef: { key: "mboxKey" }
            };
            const newFolder = {
                key: "1",
                remoteRef: {},
                name: "foo (2)",
                imapName: "foo (2)",
                path: "kung/foo (2)",
                parent: "2",
                mailboxRef: { key: "mboxKey" }
            };
            const newParent = {
                key: "2",
                remoteRef: { uid: "uid-2" },
                name: "kung",
                path: "kung",
                mailboxRef: { key: "mboxKey" }
            };

            store.commit(ADD_FOLDER, oldFolder);
            store.commit(ADD_FOLDER, existingFolder);
            store.commit(ADD_FOLDER, existingFolder2);
            store.commit(ADD_FOLDER, newParent);
            await store.dispatch(MOVE_FOLDER, { folder: oldFolder, parent: newParent, mailbox });
            expect(api.updateFolder).toHaveBeenCalledWith(mailbox, {
                internalId: undefined,
                uid: "1",
                value: {
                    fullName: "kung/foo (2)",
                    name: "foo (2)",
                    parentUid: "2"
                }
            });
            expect(store.state[newFolder.key]).toEqual(newFolder);
        });
        test("move folder with failure", async () => {
            const mailbox = {
                type: "",
                name: "",
                remoteRef: {}
            };
            const oldFolder = {
                key: "1",
                remoteRef: {},
                name: "bar",
                path: "foobaz/bar",
                parent: "-1",
                mailboxRef: { key: "mboxKey" }
            };

            const newParent = {
                key: "2",
                remoteRef: { uid: "uid-2" },
                name: "foo",
                path: "foo",
                mailboxRef: { key: "mboxKey" }
            };

            store.commit(ADD_FOLDER, oldFolder);
            store.commit(ADD_FOLDER, newParent);
            api.updateFolder.mockRejectedValue(new Error("Mocked rejection"));
            store.commit(ADD_FOLDER, oldFolder);
            expect.assertions(2);
            try {
                await store.dispatch(MOVE_FOLDER, { folder: oldFolder, parent: newParent, mailbox });
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state[oldFolder.key]).toEqual(oldFolder);
            }
        });
    });
    describe("REMOVE_FOLDER", () => {
        test("Remove folder with children", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };
            const mailboxRef = { key: 1 };
            const folder = { key: "1", imapName: "foo", name: "foo", path: "baz", mailboxRef };
            const childFolder1 = {
                key: "2",
                imapName: "foo",
                name: "child1",
                path: "baz/child1",
                parent: "1",
                mailboxRef
            };
            const childFolder2 = {
                key: "3",
                imapName: "foo",
                name: "child2",
                path: "baz/child2",
                parent: "1",
                mailboxRef
            };
            const anotherfolder = { key: "4", imapName: "foo", name: "another", path: "another", mailboxRef };
            store.commit(ADD_FOLDER, folder);
            store.commit(ADD_FOLDER, childFolder1);
            store.commit(ADD_FOLDER, childFolder2);
            store.commit(ADD_FOLDER, anotherfolder);

            const promise = await store.dispatch(REMOVE_FOLDER, { folder, mailbox });

            expect(api.deleteFolder).toHaveBeenCalledTimes(1);
            expect(api.deleteFolder).toHaveBeenCalledWith(mailbox, folder);
            expect(store.state).toEqual({ [anotherfolder.key]: anotherfolder });
            await promise;
            expect(store.state).toEqual({ [anotherfolder.key]: anotherfolder });
        });
        test("Remove folder with optimistic return", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };

            const folder1 = { key: "1", imapName: "foo", name: "foo", path: "baz", mailboxRef: { key: 1 } };
            store.commit(ADD_FOLDER, folder1);
            const folder2 = { key: "2", imapName: "foo", name: "bar", path: "baz", mailboxRef: { key: 1 } };
            store.commit(ADD_FOLDER, folder2);
            const promise = await store.dispatch(REMOVE_FOLDER, { folder: folder1, mailbox });
            expect(api.deleteFolder).toHaveBeenLastCalledWith(mailbox, folder1);
            expect(store.state).toEqual({ [folder2.key]: folder2 });
            await promise;
            expect(store.state).toEqual({ [folder2.key]: folder2 });
        });
        test("Remove folder optimistic return with failure", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };

            const folder1 = { key: "1", imapName: "foo", name: "foo", path: "baz", mailboxRef: { key: 1 } };
            store.commit(ADD_FOLDER, folder1);
            const folder2 = { key: "2", imapName: "bar", name: "bar", path: "baz", mailboxRef: { key: 1 } };
            store.commit(ADD_FOLDER, folder2);
            api.deleteFolder.mockRejectedValue(new Error("Mocked rejection"));
            expect.assertions(2);
            try {
                await store.dispatch(REMOVE_FOLDER, { folder: folder1, mailbox });
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state).toEqual({ [folder1.key]: folder1, [folder2.key]: folder2 });
            }
        });
    });
    describe("MARK_FOLDER_AS_READ", () => {
        test("Api is called", () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            const mailboxRef = { key: 1 };

            const folder = {
                key: "1",
                imapName: "foo",
                name: "foo",
                path: "baz",
                remoteRef: { uid: "uid" },
                unread: 10,
                mailboxRef
            };
            store.commit(ADD_FOLDER, folder);
            store.dispatch(MARK_FOLDER_AS_READ, { folder, mailbox });
            expect(api.markAsRead).toHaveBeenCalledWith(mailbox, folder);
        });
        test("Set unread count optimistically", () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            store.dispatch(MARK_FOLDER_AS_READ, { folder: { key: "1", remoteRef: { uid: "uid" } }, mailbox });
            expect(store.state["1"].unread).toEqual(0);
        });
        test("Set unread to 0 on success", async () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            await store.dispatch(MARK_FOLDER_AS_READ, { folder: { key: "1", remoteRef: { uid: "uid" } }, mailbox });
            expect(store.state["1"].unread).toEqual(0);
        });
        test("Reset unread count on error", async () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: { uid: "uid" }, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.markAsRead.mockRejectedValue(new Error("Mocked rejection"));
            try {
                await store.dispatch(MARK_FOLDER_AS_READ, { folder, mailbox });
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state["1"].unread).toEqual(10);
            }
        });
    });
    describe("EMPTY_FOLDER", () => {
        test("Set unread to 0", async () => {
            const mailbox = {
                type: "users",
                name: "bar",
                remoteRef: {}
            };
            const folder = { key: "1", imapName: "foo", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            await store.dispatch(EMPTY_FOLDER, { folder: { key: "1", remoteRef: { uid: "uid" } }, mailbox });
            expect(store.state["1"].unread).toEqual(0);
        });
    });
    describe("UNREAD_FOLDER_COUNT", () => {
        test("Api is called", () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: { uid: "uid" }, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.unreadCount.mockReturnValue({ total: 12 });
            store.dispatch(UNREAD_FOLDER_COUNT, folder);
            expect(api.unreadCount).toHaveBeenCalledWith(folder);
        });
        test("Set unread count on success", async () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.unreadCount.mockReturnValue({ total: 12 });
            await store.dispatch(UNREAD_FOLDER_COUNT, folder);
            expect(store.state["1"].unread).toEqual(12);
        });
        test("Keep unread count on error", async () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.unreadCount.mockRejectedValue(new Error("Mocked rejection"));
            try {
                await store.dispatch(UNREAD_FOLDER_COUNT, folder);
            } catch (error) {
                expect(error.message).toEqual("Mocked rejection");
            } finally {
                expect(store.state["1"].unread).toEqual(10);
            }
        });
    });
});
