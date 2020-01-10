import { getFolderByKey } from "../../../src/MailboxFoldersStore/getters/getFolderByKey";

const getters = {
    folders: [
        { name: "Folder 1 from container 1" },
        { name: "Folder 1 from container 2" },
        { name: "Folder 2 from container 1" },
        { name: "Folder 2 from container 2" }
    ]
};
const state = {
    itemKeys: ["key1", "key2", "key3", "key4"]
};

describe("[MailFoldersStore][getters] : getFolderByKey ", () => {
    test("return folder for a given key", () => {
        const folder = getFolderByKey(state, getters)("key2");
        expect(folder).toEqual({ name: "Folder 1 from container 2" });
    });
    test("return undefined if no folder match", () => {
        const folder = getFolderByKey(state, getters)("key5");
        expect(folder).toBeUndefined();
    });
});
