import { FOLDERS_BY_UPPERCASE_PATH, FOLDER_HAS_CHILDREN, FOLDER_GET_CHILDREN } from "~/getters";
import { FOLDER_GET_DESCENDANTS } from "../../types/getters";
import getters from "../getters";

describe("getters", () => {
    test("FOLDERS_BY_UPPERCASE_PATH", () => {
        const folder = {
            name: "foo",
            path: "foo",
            key: "123"
        };
        const state = {
            "123": folder
        };
        expect(getters[FOLDERS_BY_UPPERCASE_PATH](state)["foo".toUpperCase()]).toEqual(folder);
        expect(getters[FOLDERS_BY_UPPERCASE_PATH](state)["whatever".toUpperCase()]).toEqual(undefined);
    });

    test("FOLDER_HAS_CHILDREN", () => {
        const state = {
            "1": { key: "1", parent: null },
            "2": { key: "2", parent: "1" },
            "3": { key: "3", parent: "unknown" }
        };
        const fakeGetters = { [FOLDER_GET_CHILDREN]: getters[FOLDER_GET_CHILDREN](state) };
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "1" })).toEqual(true);
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "2" })).toEqual(false);
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "3" })).toEqual(false);
    });
    test("FOLDER_GET_CHILDREN", () => {
        const state = {
            "1": { key: "1", parent: null },
            "2": { key: "2", parent: "1" },
            "3": { key: "3", parent: "unknown" }
        };
        expect(getters[FOLDER_GET_CHILDREN](state)({ key: "1" })).toEqual([{ key: "2", parent: "1" }]);
        expect(getters[FOLDER_GET_CHILDREN](state)({ key: "2" })).toEqual([]);
        expect(getters[FOLDER_GET_CHILDREN](state)({ key: "3" })).toEqual([]);
    });
    test("FOLDER_GET_DESCENDANTS", () => {
        const state = {
            "1": { key: "1", parent: null },
            "2": { key: "2", parent: "1" },
            "3": { key: "3", parent: "2" }
        };
        const fakeGetters = { [FOLDER_GET_CHILDREN]: getters[FOLDER_GET_CHILDREN](state) };
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "1" })).toEqual([
            { key: "2", parent: "1" },
            { key: "3", parent: "2" }
        ]);
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "2" })).toEqual([{ key: "3", parent: "2" }]);
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "3" })).toEqual([]);
    });
});
