import { collapseFolder } from "../../src/mutations/collapseFolder";

describe("[Mail-WebappStore][mutations] : collapseFolder ", () => {
    const state = {};
    beforeEach(() => {
        state.foldersData = { key: { expanded: true }, key2: { something: "else" } };
    });
    test(" mutate folderData for the given folder ", () => {
        collapseFolder(state, "key");
        expect(state.foldersData["key"].expanded).not.toBeTruthy();
        collapseFolder(state, "key2");
        expect(state.foldersData["key2"].expanded).not.toBeTruthy();
    });
    test(" only mutate folderData of the given folder ", () => {
        collapseFolder(state, "key2");
        expect(state.foldersData["key2"].expanded).not.toBeTruthy();
    });
    test(" only mutate expanded key", () => {
        collapseFolder(state, "key2");
        expect(state.foldersData["key2"].something).toEqual("else");
    });
    test(" initiate folderData for the given folder if not present", () => {
        collapseFolder(state, "key3");
        expect(state.foldersData["key3"]).toEqual({ expanded: false });
    });
    test(" only mutate folderData ", () => {
        collapseFolder(state, "key");
        expect(Object.keys(state)).toEqual(["foldersData"]);
    });
});
