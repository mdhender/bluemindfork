import { getFolderByKey } from "../../../src/MailboxFoldersStore/getters/getFolderByKey";
import ItemUri from "@bluemind/item-uri";

const getters = {
    folders: [
        { name: "Folder 1 from container 1", uid: "key1" },
        { name: "Folder 1 from container 2", uid: "key2" },
        { name: "Folder 2 from container 1", uid: "key3" },
        { name: "Folder 2 from container 2", uid: "key4" }
    ]
};
const state = {
    itemKeys: [ItemUri.encode("key1"), ItemUri.encode("key2"), ItemUri.encode("key3"), ItemUri.encode("key4")]
};

const rootState = {
    mail: {
        folders: {}
    }
};

state.itemKeys.forEach((key, index) => {
    rootState.mail.folders[key] = getters.folders[index];
});

describe("[MailFoldersStore][getters] : getFolderByKey ", () => {
    test("return folder for a given key", () => {
        const folder = getFolderByKey(state, getters, rootState)(ItemUri.encode("key2"));
        expect(folder).toEqual({ name: "Folder 1 from container 2", uid: "key2" });
    });
    test("return undefined if no folder match", () => {
        const folder = getFolderByKey(state, getters, rootState)("key5");
        expect(folder).toBeUndefined();
    });
});
