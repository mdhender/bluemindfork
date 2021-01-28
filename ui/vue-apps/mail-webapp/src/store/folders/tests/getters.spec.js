import { FOLDERS_BY_UPPERCASE_PATH, FOLDER_HAS_CHILDREN } from "~getters";
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
        expect(getters[FOLDER_HAS_CHILDREN](state)("1")).toEqual(true);
        expect(getters[FOLDER_HAS_CHILDREN](state)("2")).toEqual(false);
        expect(getters[FOLDER_HAS_CHILDREN](state)("3")).toEqual(false);
    });
});
