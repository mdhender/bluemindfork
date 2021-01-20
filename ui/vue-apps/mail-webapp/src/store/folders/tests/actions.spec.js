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
    REMOVE_FOLDER,
    RENAME_FOLDER, 
    UNREAD_FOLDER_COUNT
} from "~actions";
import { ADD_FOLDER } from "~mutations";
import injector from "@bluemind/inject";
import apiFolders from "../../api/apiFolders";

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
        apiFolders.createNewFolder.mockClear();
        api.getAllFolders.mockClear();
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
                type: "",
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
            api.createNewFolder.mockRejectedValue(new Error("Mocked rejection"));
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
            const oldFolder = { key: "1", remoteRef: {}, name: "foo", path: "foobaz/foo" };
            const newFolder = { key: "1", remoteRef: {}, name: "bar", path: "foobaz/bar" };
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
            api.updateFolder.mockRejectedValue(new Error("Mocked rejection"));
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
    describe("REMOVE_FOLDER", () => {
        test("Remove folder with optimistic return", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };
            const folder1 = { key: "1", name: "foo", path: "baz" };
            store.commit(ADD_FOLDER, folder1);
            const folder2 = { key: "2", name: "bar", path: "baz" };
            store.commit(ADD_FOLDER, folder2);
            const promise = await store.dispatch(REMOVE_FOLDER, { folder: folder1, mailbox });
            expect(api.deleteFolder).toHaveBeenLastCalledWith(mailbox, folder1);
            expect(store.state).toEqual({ [folder2.key]: folder2 });
            await promise;
            expect(store.state).toEqual({ [folder2.key]: folder2 });
        });
        test("Remove folder optimistic return with failure", async () => {
            const mailbox = { type: "", name: "", remoteRef: {} };
            const folder1 = { key: "1", name: "foo", path: "baz" };
            store.commit(ADD_FOLDER, folder1);
            const folder2 = { key: "2", name: "bar", path: "baz" };
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
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: { uid: "uid" }, unread: 10 };
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
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.markAsRead.mockRejectedValue(new Error("Mocked rejection"));
            let failed = false;
            try {
                await store.dispatch(MARK_FOLDER_AS_READ, { folder: { remoteRef: { uid: "uid" } }, mailbox });
            } catch (e) {
                failed = true;
            } finally {
                expect(failed).toBeTruthy();
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
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            await store.dispatch(EMPTY_FOLDER, { folder: { key: "1", remoteRef: { uid: "uid" } }, mailbox });
            expect(store.state["1"].unread).toEqual(0);
        });
    });
    describe("UNREAD_FOLDER_COUNT", () => {
        test("Api is called", () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: { uid: "uid" }, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            store.dispatch(UNREAD_FOLDER_COUNT, folder);
            expect(api.unreadCount).toHaveBeenCalledWith(folder);
        });
        test("Set unread count on success", async () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.unreadCount.mockReturnValue({ total: 12 })
            await store.dispatch(UNREAD_FOLDER_COUNT, folder);
            expect(store.state["1"].unread).toEqual(12);
        });
        test("Keep unread count on error", async () => {
            const folder = { key: "1", name: "foo", path: "baz", remoteRef: {}, unread: 10 };
            store.commit(ADD_FOLDER, folder);
            api.unreadCount.mockRejectedValue(new Error("Mocked rejection"));
            let failed = false;
            try {
                await store.dispatch(UNREAD_FOLDER_COUNT, folder);
            } catch (e) {
                failed = true;
            } finally {
                expect(failed).toBeTruthy();
                expect(store.state["1"].unread).toEqual(10);
            }
        });
    });
});
