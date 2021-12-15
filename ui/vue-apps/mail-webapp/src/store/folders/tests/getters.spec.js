import { FOLDERS, FOLDERS_BY_UPPERCASE_PATH, FOLDER_HAS_CHILDREN, FOLDER_GET_CHILDREN } from "~/getters";
import { FOLDER_GET_DESCENDANTS } from "../../types/getters";
import getters from "../getters";

describe("getters", () => {
    test("FOLDERS", () => {
        const state = {
            "1": { key: "1", imapName: "1", path: "1", parent: null, mailboxRef: { key: "1" } },
            "3": { key: "3", imapName: "3", path: "1/2/3", parent: "2", mailboxRef: { key: "1" } },
            "6": { key: "6", imapName: "6", path: "6", parent: null, mailboxRef: { key: "1" } },
            "8": { key: "7", imapName: "2", path: "2", parent: "1", mailboxRef: { key: "2" } },
            "5": { key: "5", imapName: "5", path: "1/5", parent: "1", mailboxRef: { key: "1" } },
            "2": { key: "2", imapName: "2", path: "1/2", parent: "1", mailboxRef: { key: "1" } },
            "7": { key: "7", imapName: "1", path: "1", parent: null, mailboxRef: { key: "2" } },
            "4": { key: "4", imapName: "4", path: "1/2/4", parent: "2", mailboxRef: { key: "1" } }
        };
        expect(getters[FOLDERS](state)).toEqual([
            state["1"],
            state["2"],
            state["3"],
            state["4"],
            state["5"],
            state["6"],
            state["7"],
            state["8"]
        ]);
    });

    test("FOLDERS_BY_UPPERCASE_PATH", () => {
        const folder = {
            name: "foo",
            path: "foo",
            key: "123",
            mailboxRef: { key: "mbkey" }
        };
        const state = {
            "123": folder
        };
        const fakeGetters = { [FOLDERS]: getters[FOLDERS](state) };
        expect(getters[FOLDERS_BY_UPPERCASE_PATH](state, fakeGetters)["foo".toUpperCase()]).toEqual(folder);
        expect(getters[FOLDERS_BY_UPPERCASE_PATH](state, fakeGetters)["whatever".toUpperCase()]).toEqual(undefined);
    });
    test("FOLDER_HAS_CHILDREN", () => {
        const state = {
            "1": { key: "1", imapName: "1", path: "1", parent: null, mailboxRef: { key: "1" } },
            "2": { key: "2", imapName: "2", path: "1/2", parent: "1", mailboxRef: { key: "1" } },
            "3": { key: "3", imapName: "3", path: "1/2/3", parent: "unknown", mailboxRef: { key: "1" } }
        };
        const fakeGetters = { [FOLDERS]: getters[FOLDERS](state) };
        fakeGetters[FOLDER_GET_CHILDREN] = getters[FOLDER_GET_CHILDREN](state, fakeGetters);
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "1" })).toEqual(true);
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "2" })).toEqual(false);
        expect(getters[FOLDER_HAS_CHILDREN](state, fakeGetters)({ key: "3" })).toEqual(false);
    });
    test("FOLDER_GET_CHILDREN", () => {
        const state = {
            "1": { key: "1", imapName: "1", path: "1", parent: null, mailboxRef: { key: "1" } },
            "2": { key: "2", imapName: "2", path: "1/2", parent: "1", mailboxRef: { key: "1" } },
            "3": { key: "3", imapName: "3", path: "1/2/3", parent: "unknown", mailboxRef: { key: "1" } }
        };
        const fakeGetters = { [FOLDERS]: getters[FOLDERS](state) };
        expect(getters[FOLDER_GET_CHILDREN](state, fakeGetters)({ key: "1" })).toEqual([state["2"]]);
        expect(getters[FOLDER_GET_CHILDREN](state, fakeGetters)({ key: "2" })).toEqual([]);
        expect(getters[FOLDER_GET_CHILDREN](state, fakeGetters)({ key: "3" })).toEqual([]);
    });
    test("FOLDER_GET_DESCENDANTS", () => {
        const state = {
            "1": { key: "1", imapName: "1", path: "1", parent: null, mailboxRef: { key: "1" } },
            "2": { key: "2", imapName: "2", path: "1/2", parent: "1", mailboxRef: { key: "1" } },
            "3": { key: "3", imapName: "3", path: "1/2/3", parent: "2", mailboxRef: { key: "1" } }
        };
        const fakeGetters = { [FOLDERS]: getters[FOLDERS](state) };
        fakeGetters[FOLDER_GET_CHILDREN] = getters[FOLDER_GET_CHILDREN](state, fakeGetters);
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "1" })).toEqual([state["2"], state["3"]]);
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "2" })).toEqual([state["3"]]);
        expect(getters[FOLDER_GET_DESCENDANTS](state, fakeGetters)({ key: "3" })).toEqual([]);
    });
});
