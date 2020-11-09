import Vue from "vue";
import Vuex from "vuex";
import deepClone from "lodash.clonedeep";
import api from "../../api/apiFolders";
import storeConfig from "../index";
import { FETCH_FOLDERS, CREATE_FOLDER, REMOVE_FOLDER, RENAME_FOLDER } from "~actions";
import { ADD_FOLDER } from "~mutations";
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
    test("Fetch folders", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "users",
            name: "bar",
            remoteRef: {}
        };
        api.getAllFolders.mockReturnValue(require("../../tests/data/users/alice/folders.json"));
        await store.dispatch(FETCH_FOLDERS, mailbox);
        expect(store.state).toMatchSnapshot();
    });
    test("Create fetched folder", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: "",
            remoteRef: {}
        };
        const folder = { key: "foo", name: "bar", path: "baz" };
        api.createNewFolder.mockResolvedValue({ id: 1, uid: "bar-baz" });
        await store.dispatch(CREATE_FOLDER, { ...folder, mailbox });
        expect(api.createNewFolder).toHaveBeenCalledWith(mailbox, {
            internalId: null,
            uid: "foo",
            value: {
                fullName: "bar",
                name: "bar",
                parentUid: null
            }
        });
        expect(Object.keys(store.state)).toHaveLength(1);
        expect(store.state).toMatchSnapshot();
    });
    test("Create a folder with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: "",
            remoteRef: {}
        };
        api.createNewFolder.mockRejectedValue(new Error("Mocked rejection"));
        expect.assertions(2);
        try {
            await store.dispatch(CREATE_FOLDER, {
                ...{ key: "foo", name: "Foo", path: "foo", parent: null },
                mailbox
            });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
        } finally {
            expect(store.state).toEqual({});
        }
    });
    test("Rename folder", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: "",
            remoteRef: {}
        };
        const oldFolder = { key: "1", remoteRef: {}, name: "foo", path: "foobaz" };
        const newFolder = { key: "1", remoteRef: {}, name: "bar", path: "barbaz" };
        store.commit(ADD_FOLDER, oldFolder);
        await store.dispatch(RENAME_FOLDER, { folder: newFolder, mailbox });
        expect(api.updateFolder).toHaveBeenCalledWith(mailbox, {
            internalId: undefined,
            uid: "1",
            value: {
                fullName: "barbaz",
                name: "bar",
                parentUid: undefined
            }
        });
        expect(store.state).toEqual({ [newFolder.key]: newFolder });
    });
    test("Rename folder with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "", remoteRef: {} };
        const oldFolder = { key: "1", name: "foo", path: "baz", remoteRef: {} };
        const newFolder = { key: "1", name: "bar", path: "foobar", remoteRef: {} };
        api.updateFolder.mockRejectedValue(new Error("Mocked rejection"));
        store.commit(ADD_FOLDER, oldFolder);
        expect.assertions(2);
        try {
            await store.dispatch(RENAME_FOLDER, { folder: newFolder, mailbox });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
        } finally {
            expect(store.state).toEqual({ [oldFolder.key]: oldFolder });
        }
    });
    test("Remove folder with optimistic return", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "", remoteRef: {} };
        const folder1 = { key: "1", name: "foo", path: "baz" };
        store.commit(ADD_FOLDER, folder1);
        const folder2 = { key: "2", name: "bar", path: "baz" };
        store.commit(ADD_FOLDER, folder2);
        const promise = await store.dispatch(REMOVE_FOLDER, { key: folder1.key, mailbox });
        expect(api.deleteFolder).toHaveBeenLastCalledWith(mailbox, folder1);
        expect(store.state).toEqual({ [folder2.key]: folder2 });
        await promise;
        expect(store.state).toEqual({ [folder2.key]: folder2 });
    });
    test("Remove folder optimistic return with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "", remoteRef: {} };
        const folder1 = { key: "1", name: "foo", path: "baz" };
        store.commit(ADD_FOLDER, folder1);
        const folder2 = { key: "2", name: "bar", path: "baz" };
        store.commit(ADD_FOLDER, folder2);
        api.deleteFolder.mockRejectedValue(new Error("Mocked rejection"));
        expect.assertions(2);
        try {
            await store.dispatch(REMOVE_FOLDER, { key: folder1.key, mailbox });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
        } finally {
            expect(store.state).toEqual({ [folder1.key]: folder1, [folder2.key]: folder2 });
        }
    });
});
