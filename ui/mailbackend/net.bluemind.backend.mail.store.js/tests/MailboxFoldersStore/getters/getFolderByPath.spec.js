import { getFolderByPath } from "../../../src/MailboxFoldersStore/getters/getFolderByPath";

const folders = [
    { uid: "a", value: { name: "A", parentUid: null } },
    { uid: "b", value: { name: "B", parentUid: null } },
    { uid: "c", value: { name: "C", parentUid: null } },
    { uid: "a.d", value: { name: "D", parentUid: "a" } },
    { uid: "a.e", value: { name: "E", parentUid: "a" } },
    { uid: "b.f", value: { name: "F", parentUid: "b" } },
    { uid: "b.g", value: { name: "G", parentUid: "b" } },
    { uid: "b.h", value: { name: "H", parentUid: "b" } },
    { uid: "b.h.i", value: { name: "I", parentUid: "b.h" } },
    { uid: "b.h.j", value: { name: "J", parentUid: "b.h" } },
    { uid: "b.h.j.k", value: { name: "K", parentUid: "b.h.j" } },
    { uid: "a.e.l", value: { name: "L", parentUid: "a.e" } }
];
const getters = {
    getFoldersByMailbox: jest.fn().mockReturnValue(folders)
};

describe("[MailFoldersStore][getters] : getFolderByPath ", () => {
    test("return the folder at the given path ", () => {
        let folder = getFolderByPath(null, getters)("/A", "container 1");
        expect(folder).toEqual(folders[0]);
        folder = getFolderByPath(null, getters)("/A/E", "container 1");
        expect(folder).toEqual(folders[4]);
        folder = getFolderByPath(null, getters)("/B/H/J/K", "container 1");
        expect(folder).toEqual(folders[10]);
    });
    test("return root folder if no hierarchy is given", () => {
        const folder = getFolderByPath(null, getters)("C", "container 1");
        expect(folder).toEqual(folders[2]);
    });
    test("return ignore trailing slash", () => {
        const folder = getFolderByPath(null, getters)("/B/F/", "container 1");
        expect(folder).toEqual(folders[5]);
    });
    test("return null if the folder is not found", () => {
        const folder = getFolderByPath(null, getters)("/A/F", "container 1");
        expect(folder).toBeNull();
    });
});
