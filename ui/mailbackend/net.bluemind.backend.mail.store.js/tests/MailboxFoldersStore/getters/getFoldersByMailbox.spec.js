import { getFoldersByMailbox } from "../../../src/MailboxFoldersStore/getters/getFoldersByMailbox";

const getters = {
    getFolderByKey: key => {
        const folders = {
            key1: { name: "Folder 1 from container 1" },
            key2: { name: "Folder 1 from container 2" },
            key3: { name: "Folder 2 from container 1" },
            key4: { name: "Folder 2 from container 2" }
        };
        return folders[key];
    }
};
const state = {
    itemsByContainer: {
        "container 1": ["key1", "key3"],
        "container 2": ["key2", "key4"]
    }
};

describe("[MailFoldersStore][getters] : getFoldersByMailbox ", () => {
    test("return folders for a given mailbox", () => {
        const folders = getFoldersByMailbox(state, getters)("container 2");
        expect(folders).toEqual([{ name: "Folder 1 from container 2" }, { name: "Folder 2 from container 2" }]);
    });
    test("return an empty array if no container match", () => {
        const folders = getFoldersByMailbox(state, getters)("container 3");
        expect(folders).toEqual([]);
    });
});
