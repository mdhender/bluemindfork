import Vue from "vue";
import Vuex from "vuex";
import deepClone from "lodash.clonedeep";
import api from "../../api/apiFolders";
import storeConfig from "../index";
import { FETCH_FOLDERS, CREATE_FOLDER, REMOVE_FOLDER, RENAME_FOLDER } from "../actions";
import { ADD_FOLDER } from "../mutations";

Vue.use(Vuex);
jest.mock("../../api/apiFolders");

describe("actions", () => {
    test("Fetch folders", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "foo",
            name: "bar"
        };
        api.getAllFolders.mockReturnValue(require("../../tests/data/users/alice/folders.json"));
        await store.dispatch(FETCH_FOLDERS, mailbox);
        expect(store.state).toMatchSnapshot();
    });
    test("Create fetched folder", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: ""
        };
        const folder = { key: "foo", name: "bar", path: "baz" };
        api.createNewFolder.mockImplementation((mailbox, item) => {
            if (
                item.uid === folder.key &&
                item.value.name === folder.name &&
                item.value.fullName === folder.name &&
                item.value.parentUid === null
            ) {
                return Promise.resolve({ uid: `${item.value.name}-baz`, id: `${item.value.name}-baz` });
            } else {
                return Promise.reject("Invalid parameters");
            }
        });
        await store.dispatch(CREATE_FOLDER, { ...folder, mailbox });
        expect(Object.keys(store.state)).toHaveLength(1);
        expect(store.state).toMatchSnapshot();
    });
    test("Create a folder with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: ""
        };
        api.createNewFolder.mockRejectedValue(new Error("Mocked rejection"));
        try {
            await store.dispatch(CREATE_FOLDER, {
                ...{ key: "foo", name: "Foo", path: "foo", parent: null },
                mailbox
            });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
            expect(store.state).toEqual({});
        }
    });
    test("Rename folder", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = {
            type: "",
            name: ""
        };
        const oldFolder = { key: "1", name: "foo", path: "foobaz" };
        const newFolder = { key: "1", name: "bar", path: "barbaz" };
        api.updateFolder.mockImplementation((mailbox, item) => {
            if (
                item.value.name === newFolder.name &&
                item.value.fullName === newFolder.path &&
                item.value.parentUid === undefined
            )
                return Promise.resolve();
            else return Promise.reject("Invalid parameters");
        });
        store.commit(ADD_FOLDER, oldFolder);
        await store.dispatch(RENAME_FOLDER, { folder: newFolder, mailbox });
        expect(store.state).toEqual({ [newFolder.key]: newFolder });
    });
    test("Rename folder with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "" };
        const oldFolder = { key: "1", name: "foo", path: "baz" };
        const newFolder = { key: "1", name: "bar", path: "foobar" };
        api.updateFolder.mockRejectedValue(new Error("Mocked rejection"));
        store.commit(ADD_FOLDER, oldFolder);
        try {
            await store.dispatch(RENAME_FOLDER, { folder: newFolder, mailbox });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
            expect(store.state).toEqual({ [oldFolder.key]: oldFolder });
        }
    });
    test("Remove folder with optimistic return", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "" };
        const folder1 = { key: "1", name: "foo", path: "baz" };
        store.commit(ADD_FOLDER, folder1);
        const folder2 = { key: "2", name: "bar", path: "baz" };
        store.commit(ADD_FOLDER, folder2);
        api.deleteFolder.mockImplementation((mailbox, folder) => {
            if (folder.key === folder1.key) {
                expect(store.state).toEqual({ [folder2.key]: folder2 });
                return {};
            }
            throw new Error("Test Error: invalid parameters");
        });
        await store.dispatch(REMOVE_FOLDER, { key: folder1.key, mailbox });
        expect(store.state).toEqual({ [folder2.key]: folder2 });
    });
    test("Remove folder optimistic return with failure", async () => {
        const store = new Vuex.Store(deepClone(storeConfig));
        const mailbox = { type: "", name: "" };
        const folder1 = { key: "1", name: "foo", path: "baz" };
        store.commit(ADD_FOLDER, folder1);
        const folder2 = { key: "2", name: "bar", path: "baz" };
        store.commit(ADD_FOLDER, folder2);
        api.deleteFolder.mockImplementation((mailbox, folder) => {
            if (folder.key === folder1.key) {
                expect(store.state).toEqual({ [folder2.key]: folder2 });
                return {};
            }
            throw new Error("Test Error: invalid parameters");
        });
        try {
            await store.dispatch(REMOVE_FOLDER, { key: folder1.key, mailbox });
        } catch (error) {
            expect(error.message).toEqual("Mocked rejection");
            expect(store.state).toEqual({ [folder1.key]: folder1, [folder2.key]: folder2 });
        }
    });
});
