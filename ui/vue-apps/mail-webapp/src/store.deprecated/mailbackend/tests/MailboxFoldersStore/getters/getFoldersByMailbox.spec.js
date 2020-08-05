import { getFoldersByMailbox } from "../../../src/MailboxFoldersStore/getters/getFoldersByMailbox";
import ItemUri from "@bluemind/item-uri";

const getters = {
    folders: [
        { name: "Folder 1 from container 1", key: ItemUri.encode("key1", "container 1") },
        { name: "Folder 1 from container 2", key: ItemUri.encode("key2", "container 2") },
        { name: "Folder 2 from container 1", key: ItemUri.encode("key3", "container 1") },
        { name: "Folder 2 from container 2", key: ItemUri.encode("key4", "container 2") }
    ]
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
        expect(folders).toEqual([getters.folders[1], getters.folders[3]]);
    });
    test("return an empty array if no container match", () => {
        const folders = getFoldersByMailbox(state, getters)("container 3");
        expect(folders).toEqual([]);
    });
});
